# LocalStack Module

Testcontainers module for [LocalStack](http://localstack.cloud/), 'a fully functional local AWS cloud stack', to develop and test your cloud and serverless apps without actually using the cloud.

## Usage example

You can start a LocalStack container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../modules/localstack/src/test/java/org/testcontainers/localstack/LocalStackContainerTest.java) inside_block:container
<!--/codeinclude-->

Environment variables listed in the [LocalStack configuration documentation](https://docs.localstack.cloud/references/configuration/) may be used to customize LocalStack's configuration.
Use the `.withEnv(key, value)` method on `LocalStackContainer` to apply configuration settings.

!!! note
    Starting March 23, 2026, `localstack/localstack` requires authentication via a `LOCALSTACK_AUTH_TOKEN` environment variable. Without it, the container will fail to start.

    Use `.withEnv("LOCALSTACK_AUTH_TOKEN", System.getenv("LOCALSTACK_AUTH_TOKEN"))` to pass the token.
    See the [LocalStack blog post](https://blog.localstack.cloud/localstack-single-image-next-steps/) for more details.

## Creating a client using AWS SDK

<!--codeinclude-->
[AWS SDK V2](../../modules/localstack/src/test/java/org/testcontainers/localstack/LocalStackContainerTest.java) inside_block:with_aws_sdk_v2
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-localstack:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-localstack</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
