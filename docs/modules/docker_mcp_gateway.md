# Docker MCP Gateway

Testcontainers module for [Docker MCP Gateway](https://hub.docker.com/r/docker/mcp-gateway).

## DockerMcpGatewayContainer's usage examples

You can start a Docker MCP Gateway container instance from any Java application by using:

<!--codeinclude-->
[Create a DockerMcpGatewayContainer](../../core/src/test/java/org/testcontainers/containers/DockerMcpGatewayContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

*Docker MCP Gateway support is part of the core Testcontainers library.*

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

