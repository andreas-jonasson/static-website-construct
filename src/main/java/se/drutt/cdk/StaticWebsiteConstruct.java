package se.drutt.cdk;

import software.amazon.awscdk.services.certificatemanager.Certificate;
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

public class StaticWebsiteConstruct extends Construct {
    
    private final Bucket websiteBucket;
    private final Distribution distribution;
    
    public static class StaticWebsiteProps {
        private final String bucketName;
        private final String domainName;
        private final String hostedZoneId;
        private final String zoneName;
        private final String webContentPath;
        private final Certificate certificate;
        
        private StaticWebsiteProps(Builder builder) {
            this.bucketName = builder.bucketName;
            this.domainName = builder.domainName;
            this.hostedZoneId = builder.hostedZoneId;
            this.zoneName = builder.zoneName;
            this.webContentPath = builder.webContentPath;
            this.certificate = builder.certificate;
        }
        
        public static class Builder {
            private String bucketName;
            private String domainName;
            private String hostedZoneId;
            private String zoneName;
            private String webContentPath;
            private Certificate certificate;
            
            public Builder bucketName(String bucketName) {
                this.bucketName = bucketName;
                return this;
            }
            
            public Builder domainName(String domainName) {
                this.domainName = domainName;
                return this;
            }
            
            public Builder hostedZoneId(String hostedZoneId) {
                this.hostedZoneId = hostedZoneId;
                return this;
            }
            
            public Builder zoneName(String zoneName) {
                this.zoneName = zoneName;
                return this;
            }
            
            public Builder webContentPath(String webContentPath) {
                this.webContentPath = webContentPath;
                return this;
            }
            
            public Builder certificate(Certificate certificate) {
                this.certificate = certificate;
                return this;
            }
            
            public StaticWebsiteProps build() {
                return new StaticWebsiteProps(this);
            }
        }
    }
    
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
    
    public Bucket getWebsiteBucket() {
        return websiteBucket;
    }
    
    public Distribution getDistribution() {
        return distribution;
    }
    
    public String getDistributionUrl() {
        return "https://" + distribution.getDistributionDomainName();
    }
}