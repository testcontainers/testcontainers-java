# AWS Emulator

!!! note
This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for [AWS Emulator](https://github.com/aws/aws-emulator).

## AwsEmulatorContainer's usage examples

You can start an AWS Emulator container instance from any Java application by using:

<!--codeinclude-->
[AWS Emulator container](../../modules/aws-emulator/src/test/java/org/testcontainers/awsemulator/AwsEmulatorContainerTest.java) inside_block:container
<!--/codeinclude-->

The container automatically configures the recommended 2 GiB memory limit, disables usage telemetry by default, waits for the HTTPS `/__admin/health` endpoint, and extracts the generated PKCS12 truststore (`getTrustStorePath()`) and SigV4 credentials (`getAccessKey()`, `getSecretKey()`).

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:testcontainers-aws-emulator:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-aws-emulator</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
