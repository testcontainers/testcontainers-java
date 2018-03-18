# Testcontainers LocalStack AWS testing module

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

## Dependency information

Replace `VERSION` with the [latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22).

### Maven
```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>localstack</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'localstack', version: 'VERSION'
```
