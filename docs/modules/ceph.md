# Ceph Containers

Testcontainers can be used to automatically instantiate and manage [Ceph](https://ceph.io) containers.

## Usage example

Create a `CephContainer` to use it in your tests:
<!--codeinclude-->
[Starting a Ceph container](../../modules/ceph/src/test/java/org/testcontainers/containers/CephContainerTest.java) inside_block:cephContainer
<!--/codeinclude-->

The [AWS Java SDK](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3.html) can be configured with the container as such:
<!--codeinclude-->
[Configuring a Ceph client](../../modules/ceph/src/test/java/org/testcontainers/containers/CephContainerTest.java) inside_block:configuringClient
<!--/codeinclude-->

If needed the username and password can be overridden as such:
<!--codeinclude-->
[Overriding a Ceph container](../../modules/ceph/src/test/java/org/testcontainers/containers/CephContainerTest.java) inside_block:cephOverrides
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
     testImplementation "org.testcontainers:ceph:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>ceph</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
