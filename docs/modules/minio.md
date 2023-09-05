# MinIO Containers

Test containers can be used to automatically instantiate and manage [MinIO](https://min.io) containers.

## Usage example

Create a `MinIOContainer` to use it in your tests:
<!--codeinclude-->
[Starting a MinIO container](../../modules/minio/src/test/java/org/testcontainers/containers/MinIOContainerTest.java) inside_block:minioContainer
<!--/codeinclude-->

The [MinIO Java client](https://min.io/docs/minio/linux/developers/java/API.html) can be configured with the container as such:
<!--codeinclude-->
[Configuring a MinIO client](../../modules/minio/src/test/java/org/testcontainers/containers/MinIOContainerTest.java) inside_block:configuringClient
<!--/codeinclude-->

If needed the username and password can be overridden as such:
<!--codeinclude-->
[Overriding a MinIO container](../../modules/minio/src/test/java/org/testcontainers/containers/MinIOContainerTest.java) inside_block:minioOverrides
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:minio:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
<groupId>org.testcontainers</groupId>
<artifactId>minio</artifactId>
<version>{{latest_version}}</version>
<scope>test</scope>
</dependency>
```
