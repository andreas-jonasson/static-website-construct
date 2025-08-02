package se.drutt.cdk;

import software.amazon.awscdk.services.certificatemanager.ICertificate;
import software.amazon.awscdk.services.cloudfront.*;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOrigin;
import software.amazon.awscdk.services.cloudfront.origins.S3BucketOriginWithOACProps;
import software.amazon.awscdk.services.route53.*;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.BucketAccessControl;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.amazon.awscdk.services.route53.targets.CloudFrontTarget;
import software.constructs.Construct;

import java.util.List;

/**
 * A reusable CDK construct for static websites with CloudFront and custom domains.
 * <p>
 * This construct handles:
 * <ul>
 *   <li>S3 bucket for website hosting</li>
 *   <li>CloudFront distribution with custom domain</li>
 *   <li>ACM certificate (in us-east-1 region)</li>
 *   <li>Route53 DNS records</li>
 *   <li>Content deployment</li>
 * </ul>
 * <p>
 * The construct extends Construct (not Stack) for better composability.
 * 
 * @author andreas.jonasson@gmail.com
 * @version 1.0
 */
@SuppressWarnings("unused")
public class StaticWebsiteConstruct extends Construct {
    
    private final Bucket websiteBucket;
    private final Distribution distribution;
    
    /**
     * Configuration properties for the StaticWebsiteConstruct.
     */
    public static class StaticWebsiteProps {
        private final String bucketName;
        private final String domainName;
        private final String hostedZoneId;
        private final String zoneName;
        private final String webContentPath;
        private final ICertificate certificate;
        
        private StaticWebsiteProps(Builder builder) {
            this.bucketName = builder.bucketName;
            this.domainName = builder.domainName;
            this.hostedZoneId = builder.hostedZoneId;
            this.zoneName = builder.zoneName;
            this.webContentPath = builder.webContentPath;
            this.certificate = builder.certificate;
        }
        
        /**
         * Builder for StaticWebsiteProps.
         */
        public static class Builder {
            private String bucketName;
            private String domainName;
            private String hostedZoneId;
            private String zoneName;
            private String webContentPath;
            private ICertificate certificate;
            
            /**
             * Sets the S3 bucket name.
             * @param bucketName Name for the S3 bucket
             * @return this builder
             */
            public Builder bucketName(String bucketName) {
                this.bucketName = bucketName;
                return this;
            }
            
            /**
             * Sets the domain name for the website.
             * @param domainName Domain name (e.g., www.example.com)
             * @return this builder
             */
            public Builder domainName(String domainName) {
                this.domainName = domainName;
                return this;
            }
            
            /**
             * Sets the Route53 hosted zone ID.
             * @param hostedZoneId ID of the Route53 hosted zone
             * @return this builder
             */
            public Builder hostedZoneId(String hostedZoneId) {
                this.hostedZoneId = hostedZoneId;
                return this;
            }
            
            /**
             * Sets the zone name for the Route53 hosted zone.
             * @param zoneName Name of the Route53 zone (e.g., example.com)
             * @return this builder
             */
            public Builder zoneName(String zoneName) {
                this.zoneName = zoneName;
                return this;
            }
            
            /**
             * Sets the path to the website content for deployment.
             * @param webContentPath Path to the website content directory
             * @return this builder
             */
            public Builder webContentPath(String webContentPath) {
                this.webContentPath = webContentPath;
                return this;
            }
            
            /**
             * Sets the ACM certificate for HTTPS.
             * @param certificate ACM certificate (must be in us-east-1 for CloudFront)
             * @return this builder
             */
            public Builder certificate(ICertificate certificate) {
                this.certificate = certificate;
                return this;
            }
            
            /**
             * Builds the StaticWebsiteProps object.
             * @return A new StaticWebsiteProps instance
             */
            public StaticWebsiteProps build() {
                return new StaticWebsiteProps(this);
            }
        }
    }
    
    /**
     * Creates a new StaticWebsiteConstruct.
     * 
     * @param scope The parent construct
     * @param id The construct ID
     * @param props Configuration properties for the static website
     */
    public StaticWebsiteConstruct(final Construct scope, final String id, final StaticWebsiteProps props) {
        super(scope, id);
        
        // Create S3 bucket
        websiteBucket = Bucket.Builder.create(this, "WebsiteBucket")
                .bucketName(props.bucketName)
                .accessControl(BucketAccessControl.PRIVATE)
                .build();

        // Create CloudFront Origin Access Identity
        OriginAccessIdentity originAccessIdentity = OriginAccessIdentity.Builder.create(this, "OriginAccessIdentity")
                .comment("CloudFront access to S3")
                .build();

        websiteBucket.grantRead(originAccessIdentity);

        IOrigin s3Origin = S3BucketOrigin.withOriginAccessControl(websiteBucket, S3BucketOriginWithOACProps.builder()
                .originAccessLevels(List.of(AccessLevel.READ, AccessLevel.LIST))
                .build());

        // Look up hosted zone
        IHostedZone hostedZone = HostedZone.fromHostedZoneAttributes(this, "HostedZone",
                HostedZoneAttributes.builder()
                        .hostedZoneId(props.hostedZoneId)
                        .zoneName(props.zoneName)
                        .build());
                
        // Create CloudFront distribution
        distribution = Distribution.Builder.create(this, "Distribution")
                .defaultBehavior(BehaviorOptions.builder()
                        .origin(s3Origin)
                        .allowedMethods(AllowedMethods.ALLOW_GET_HEAD)
                        .cachePolicy(CachePolicy.CACHING_OPTIMIZED)
                        .build())
                .defaultRootObject("index.html")
                .domainNames(List.of(props.domainName))
                .certificate(props.certificate)
                .errorResponses(List.of(
                        ErrorResponse.builder()
                                .httpStatus(404)
                                .responseHttpStatus(200)
                                .responsePagePath("/index.html")
                                .build()
                ))
                .build();

        // Deploy website assets
        BucketDeployment.Builder.create(this, "DeployWebsite")
                .sources(List.of(Source.asset(props.webContentPath)))
                .destinationBucket(websiteBucket)
                .distribution(distribution)
                .distributionPaths(List.of("/*"))
                .build();

        // Create Route53 record
        ARecord.Builder.create(this, "AliasRecord")
                .zone(hostedZone)
                .recordName(props.domainName)
                .target(RecordTarget.fromAlias(new CloudFrontTarget(distribution)))
                .build();
    }
    
    /**
     * Gets the S3 bucket used for website content.
     * @return The website S3 bucket
     */
    public Bucket getWebsiteBucket() {
        return websiteBucket;
    }
    
    /**
     * Gets the CloudFront distribution.
     * @return The CloudFront distribution
     */
    public Distribution getDistribution() {
        return distribution;
    }
    
    /**
     * Gets the CloudFront distribution URL.
     * @return The CloudFront distribution URL
     */
    public String getDistributionUrl() {
        return "https://" + distribution.getDistributionDomainName();
    }
}