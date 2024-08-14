# LocalStack Module

Testcontainers module for [LocalStack](http://localstack.cloud/), 'a fully functional local AWS cloud stack', to develop and test your cloud and serverless apps without actually using the cloud.

## Usage example

Running LocalStack as a stand-in for AWS S3 during a test:

```java
DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.5.0");

@Rule
public LocalStackContainer localstack = new LocalStackContainer(localstackImage)
        .withServices(S3);
```

## Creating a client using AWS SDK

<!--codeinclude-->
[AWS SDK V1](../../modules/localstack/src/test/java/org/testcontainers/containers/localstack/LocalstackContainerTest.java) inside_block:with_aws_sdk_v1
<!--/codeinclude-->

<!--codeinclude-->
[AWS SDK V2](../../modules/localstack/src/test/java/org/testcontainers/containers/localstack/LocalstackContainerTest.java) inside_block:with_aws_sdk_v2
<!--/codeinclude-->

Environment variables listed in [Localstack's README](https://github.com/localstack/localstack#configurations) may be used to customize Localstack's configuration. 
Use the `.withEnv(key, value)` method on `LocalStackContainer` to apply configuration settings.

## `HOSTNAME_EXTERNAL` and hostname-sensitive services

Some Localstack APIs, such as SQS, require the container to be aware of the hostname that it is accessible on - for example, for construction of queue URLs in responses.

Testcontainers will inform Localstack of the best hostname automatically, using the `HOSTNAME_EXTERNAL` environment variable:

* when running the Localstack container directly without a custom network defined, it is expected that all calls to the container will be from the test host. As such, the container address will be used (typically localhost or the address where the Docker daemon is running).

    <!--codeinclude-->
    [Localstack container running without a custom network](../../modules/localstack/src/test/java/org/testcontainers/containers/localstack/LocalstackContainerTest.java) inside_block:without_network
    <!--/codeinclude-->

* when running the Localstack container [with a custom network defined](/features/networking/#advanced-networking), it is expected that all calls to the container will be **from other containers on that network**. `HOSTNAME_EXTERNAL` will be set to the *last* network alias that has been configured for the Localstack container.

    <!--codeinclude-->
    [Localstack container running with a custom network](../../modules/localstack/src/test/java/org/testcontainers/containers/localstack/LocalstackContainerTest.java) inside_block:with_network
    <!--/codeinclude-->

* Other usage scenarios, such as where the Localstack container is used from both the test host and containers on a custom network are not automatically supported. If you have this use case, you should set `HOSTNAME_EXTERNAL` manually.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:localstack:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>localstack</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
