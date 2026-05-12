**Build and install locally**:
   ```
   mvn clean install
   ```

## Using in Other Projects

1. **Add dependency** to your project's `pom.xml`:
   ```xml
   <dependency>
       <groupId>se.drutt.cdk</groupId>
       <artifactId>static-website</artifactId>
       <version>1.0</version>
   </dependency>
   ```

2. **Basic usage**:
   ```java
   StaticWebsiteConstruct website = new StaticWebsiteConstruct(this, "Website", 
       new StaticWebsiteConstruct.StaticWebsiteProps.Builder()
           .bucketName("my-website-bucket")
           .domainName("myapp.example.com")
           .hostedZoneId("Z1234567890ABC")
           .zoneName("example.com")
           .webContentPath("./dist")
           .certificate(certificate)
           .build());
   ```

3. **Advanced usage with cost optimization**:
   ```java
   StaticWebsiteConstruct website = new StaticWebsiteConstruct(this, "Website", 
       new StaticWebsiteConstruct.StaticWebsiteProps.Builder()
           .bucketName("my-website-bucket")
           .domainName("myapp.example.com")
           .hostedZoneId("Z1234567890ABC")
           .zoneName("example.com")
           .webContentPath("./dist")
           .certificate(certificate)
           .priceClass(PriceClass.PRICE_CLASS_100)  // Use only North America & Europe edge locations
           .enableLogging(false)  // Disable logging to reduce costs
           .build());
   ```

## Features

- **Security**: S3 encryption, versioning, block public access, HTTPS enforcement, security headers (HSTS, CSP, X-Frame-Options)
- **Performance**: Gzip/Brotli compression, HTTP/2 and HTTP/3 support, CloudFront caching
- **SPA Support**: Automatic 403/404 error handling for single-page applications
- **Cost Optimized**: Defaults to PRICE_CLASS_100 (North America & Europe only), optional logging
- **Modern**: Uses Origin Access Control (OAC) instead of legacy OAI
