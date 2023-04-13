# Solace Container

This module helps running [Solace PubSub+](https://solace.com/products/event-broker/software/) using Testcontainers.

Note that it's based on the [official Docker image](https://hub.docker.com/r/solace/solace-pubsub-standard).

## Usage example

You can start a solace container instance from any Java application by using:

<!--codeinclude-->
[Solace container setup with simple authentication](../../modules/solace/src/test/java/org/testcontainers/solace/SolaceContainerSMFTest.java) inside_block:solaceContainerSetup
<!--/codeinclude-->

<!--codeinclude-->
[Solace container setup with SSL](../../modules/solace/src/test/java/org/testcontainers/solace/SolaceContainerSMFTest.java) inside_block:solaceContainerUsageSSL
<!--/codeinclude-->

<!--codeinclude-->
[Using a Solace container](../../modules/solace/src/test/java/org/testcontainers/solace/SolaceContainerAMQPTest.java) inside_block:solaceContainerUsage
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:solace:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>solace</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
