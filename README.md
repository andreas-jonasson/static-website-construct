**Build and install locally**:
   ```
   mvn clean install
   ```

## Using in Other Projects

1. **Add dependency** to your project's `pom.xml`:
   ```xml
   <dependency>
       <groupId>se.drutt.cdk</groupId>
       <artifactId>static-website-construct</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. **Use in your stacks**:
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
