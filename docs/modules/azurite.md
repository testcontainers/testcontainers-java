# Azurite Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

Testcontainers module for the Microsoft [Azurite](https://github.com/Azure/Azurite).

## Usage example

You can start an azurite container instance from any Java application by using:

<!--codeinclude-->
[Using a Azurite container](../../modules/azurite/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:azuriteContainerUsage
<!--/codeinclude-->

You can create a blob storage client from an azurite container instance by using:

<!--codeinclude-->
[Create a blob storage client](../../modules/azurite/src/test/java/org/testcontainers/containers/AzuriteContainerTest.java) inside_block:createBlobStorageClient
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:azurite:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>azurite</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

