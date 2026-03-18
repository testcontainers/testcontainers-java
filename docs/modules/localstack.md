# LocalStack Module

Testcontainers module for [LocalStack](http://localstack.cloud/), 'a fully functional local AWS cloud stack', to develop and test your cloud and serverless apps without actually using the cloud.

!!! warning "Auth Token Required for `localstack/localstack:latest`"
    Starting March 23, 2026, `localstack/localstack:latest` requires a
    `LOCALSTACK_AUTH_TOKEN` environment variable. To continue without an
    auth token, pin to a pre-change version such as `4.14.0`.
    See the [LocalStack Auth Token documentation](https://docs.localstack.cloud/getting-started/auth-token/)
    for details.

## Usage example

You can start a LocalStack container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../modules/localstack/src/test/java/org/testcontainers/localstack/LocalStackContainerTest.java) inside_block:container
<!--/codeinclude-->

### Using an Auth Token

For LocalStack versions that require authentication, set your auth token:

```java
LocalStackContainer localstack = new LocalStackContainer(
    DockerImageName.parse("localstack/localstack:latest")
).withEnv("LOCALSTACK_AUTH_TOKEN", System.getenv("LOCALSTACK_AUTH_TOKEN"));
```

### Pinning to a Pre-Change Version

To continue without an auth token, pin to a pre-change version such as `4.14.0`:

```java
LocalStackContainer localstack = new LocalStackContainer(
    DockerImageName.parse("localstack/localstack:4.14.0")
);
```

## Creating a client using AWS SDK

<!--codeinclude-->
[AWS SDK V2](../../modules/localstack/src/test/java/org/testcontainers/localstack/LocalStackContainerTest.java) inside_block:with_aws_sdk_v2
<!--/codeinclude-->

Environment variables listed in [Localstack's README](https://github.com/localstack/localstack#configurations) may be used to customize Localstack's configuration. 
Use the `.withEnv(key, value)` method on `LocalStackContainer` to apply configuration settings.

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
