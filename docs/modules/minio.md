# MinIO Containers

Testcontainers can be used to automatically instantiate and manage [MinIO](https://min.io) containers.

!!! note
    Upstream MinIO stopped publishing free community builds in late 2025 and archived the
    [minio/minio](https://github.com/minio/minio) repository in 2026; the `minio/minio` image is no longer available on
    Docker Hub. The module therefore also accepts [Silo](https://github.com/pgsty/silo) (`pgsty/silo`), a
    community-maintained, AGPL-licensed fork of MinIO by the [Pigsty](https://pigsty.io) project, which is used in the
    examples below. See the [Silo manifesto](https://silo.pigsty.io/about/manifesto/) for background on the fork.

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
     testImplementation "org.testcontainers:testcontainers-minio:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-minio</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
