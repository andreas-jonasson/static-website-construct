# Static Website CDK Construct

A production-ready AWS CDK construct for deploying static websites with CloudFront, S3, custom domains, and comprehensive security features.

## Features

### Security
- ✅ S3 server-side encryption (SSE-S3)
- ✅ S3 bucket versioning enabled
- ✅ Block all public S3 access
- ✅ HTTPS enforcement (redirects HTTP to HTTPS)
- ✅ Modern Origin Access Control (OAC) instead of legacy OAI
- ✅ Comprehensive security headers:
  - Strict-Transport-Security (HSTS)
  - X-Content-Type-Options
  - X-Frame-Options (DENY)
  - Referrer-Policy
  - X-XSS-Protection
  - Permissions-Policy
- ✅ TLS 1.2 minimum protocol version
- ✅ Input validation for all required properties

### Performance
- ✅ Automatic Gzip/Brotli compression
- ✅ HTTP/2 and HTTP/3 support
- ✅ CloudFront caching with optimized cache policy
- ✅ IPv6 enabled

### SPA Support
- ✅ Automatic 403/404 error handling for single-page applications
- ✅ Routes all errors to index.html for client-side routing

### Cost Optimization
- ✅ Defaults to PRICE_CLASS_100 (North America & Europe edge locations only)
- ✅ Optional CloudFront access logging (disabled by default)
- ✅ Configurable price class for global reach vs. cost trade-off

## Installation

### Build and Install Locally

```bash
mvn clean install
```

### Add Dependency to Your Project

Add to your project's `pom.xml`:

```xml
<dependency>
    <groupId>se.drutt.cdk</groupId>
    <artifactId>static-website</artifactId>
    <version>1.0</version>
</dependency>
```

## Usage

### Prerequisites

You need an ACM certificate in the **us-east-1** region for CloudFront:

```java
import software.amazon.awscdk.services.certificatemanager.Certificate;
import software.amazon.awscdk.services.certificatemanager.CertificateValidation;
import software.amazon.awscdk.services.route53.HostedZone;

// Look up your hosted zone
IHostedZone hostedZone = HostedZone.fromHostedZoneAttributes(this, "HostedZone",
    HostedZoneAttributes.builder()
        .hostedZoneId("Z1234567890ABC")
        .zoneName("example.com")
        .build());

// Create certificate (must be in us-east-1 for CloudFront)
ICertificate certificate = Certificate.Builder.create(this, "Certificate")
    .domainName("www.example.com")
    .validation(CertificateValidation.fromDns(hostedZone))
    .build();

// Or import existing certificate
ICertificate certificate = Certificate.fromCertificateArn(this, "Certificate",
    "arn:aws:acm:us-east-1:123456789012:certificate/abc123...");
```

### Basic Usage

```java
import se.drutt.cdk.StaticWebsiteConstruct;

StaticWebsiteConstruct website = new StaticWebsiteConstruct(this, "Website", 
    new StaticWebsiteConstruct.StaticWebsiteProps.Builder()
        .bucketName("my-website-bucket")
        .domainName("www.example.com")
        .hostedZoneId("Z1234567890ABC")
        .zoneName("example.com")
        .webContentPath("./dist")
        .certificate(certificate)
        .build());
```

### Advanced Usage with Cost Optimization

```java
import software.amazon.awscdk.services.cloudfront.PriceClass;

StaticWebsiteConstruct website = new StaticWebsiteConstruct(this, "Website", 
    new StaticWebsiteConstruct.StaticWebsiteProps.Builder()
        .bucketName("my-website-bucket")
        .domainName("www.example.com")
        .hostedZoneId("Z1234567890ABC")
        .zoneName("example.com")
        .webContentPath("./dist")
        .certificate(certificate)
        .priceClass(PriceClass.PRICE_CLASS_100)  // North America & Europe only (default)
        .enableLogging(false)  // Disable logging to reduce costs (default)
        .build());
```

### Global Distribution

For worldwide reach, use all edge locations:

```java
StaticWebsiteConstruct website = new StaticWebsiteConstruct(this, "Website", 
    new StaticWebsiteConstruct.StaticWebsiteProps.Builder()
        .bucketName("my-website-bucket")
        .domainName("www.example.com")
        .hostedZoneId("Z1234567890ABC")
        .zoneName("example.com")
        .webContentPath("./dist")
        .certificate(certificate)
        .priceClass(PriceClass.PRICE_CLASS_ALL)  // All edge locations worldwide
        .enableLogging(true)  // Enable CloudFront access logs
        .build());
```

### Accessing Created Resources

```java
StaticWebsiteConstruct website = new StaticWebsiteConstruct(this, "Website", props);

// Access the S3 bucket
Bucket bucket = website.getWebsiteBucket();

// Access the CloudFront distribution
Distribution distribution = website.getDistribution();

// Get the CloudFront URL
String url = website.getDistributionUrl();
```

## Configuration Options

| Property | Type | Required | Default | Description |
|----------|------|----------|---------|-------------|
| `bucketName` | String | Yes | - | S3 bucket name for website content |
| `domainName` | String | Yes | - | Custom domain name (e.g., www.example.com) |
| `hostedZoneId` | String | Yes | - | Route53 hosted zone ID |
| `zoneName` | String | Yes | - | Route53 zone name (e.g., example.com) |
| `webContentPath` | String | Yes | - | Path to website content directory |
| `certificate` | ICertificate | Yes | - | ACM certificate (must be in us-east-1) |
| `priceClass` | PriceClass | No | PRICE_CLASS_100 | CloudFront price class |
| `enableLogging` | Boolean | No | false | Enable CloudFront access logging |

## Price Classes

- **PRICE_CLASS_100**: North America & Europe (lowest cost)
- **PRICE_CLASS_200**: North America, Europe, Asia, Middle East, and Africa
- **PRICE_CLASS_ALL**: All edge locations worldwide (highest cost)

## What Gets Created

1. **S3 Bucket** - Encrypted, versioned, private bucket for website content
2. **CloudFront Distribution** - CDN with custom domain, HTTPS, and security headers
3. **Origin Access Control** - Secure access from CloudFront to S3
4. **Route53 A Record** - DNS alias pointing to CloudFront distribution
5. **Bucket Deployment** - Automatic deployment of website content
6. **Log Bucket** (optional) - Separate bucket for CloudFront access logs

## Security Best Practices

This construct implements AWS security best practices:

- S3 bucket is private with no public access
- All traffic is encrypted in transit (HTTPS only)
- Data at rest is encrypted (S3 SSE)
- Bucket versioning protects against accidental deletions
- Security headers protect against common web vulnerabilities
- Modern OAC instead of legacy OAI for S3 access
- TLS 1.2 minimum for CloudFront connections

## Requirements

- AWS CDK 2.238.0 or later
- Java 17 or later
- Maven 3.6 or later

## License

AGPL-3.0

## Author

andreas.jonasson@gmail.com
