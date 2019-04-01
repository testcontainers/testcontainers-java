# Localstack Module

Testcontainers module for the Atlassian's [LocalStack](https://github.com/localstack/localstack), 'a fully functional local AWS cloud stack'.

## Usage example

Running LocalStack as a stand-in for AWS S3 during a test:

```java
public class SomeTest {

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer()
            .withServices(S3);
    
    @Test
    public void someTestMethod() {
        AmazonS3 s3 = AmazonS3ClientBuilder
                        .standard()
                        .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
                        .withCredentials(localstack.getDefaultCredentialsProvider())
                        .build();
        
                s3.createBucket("foo");
                s3.putObject("foo", "bar", "baz");
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:localstack:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
