# Mockserver Module

!!! note "Deprecated — use the MockServer-maintained module for new projects"
    This bundled module is deprecated. For new projects, prefer the MockServer-maintained
    module [`org.mock-server:mockserver-testcontainers`](https://www.mock-server.com/mock_server/mockserver_testcontainers.html)
    (class `org.mockserver.testcontainers.MockServerContainer`). It tracks current MockServer
    releases, derives its image tag from the client library so the container and client stay in
    lockstep, and adds configuration helpers (DNS, transparent proxy, HTTP/3, initialization JSON,
    log level, arbitrary properties) plus direct `MockServerClient` wiring. This page documents the
    legacy bundled module.

Mock Server can be used to mock HTTP services by matching requests against user-defined expectations.

## Usage example

The following example shows how to start Mockserver.

<!--codeinclude-->
[Creating a MockServer container](../../modules/mockserver/src/test/java/org/testcontainers/mockserver/MockServerContainerTest.java) inside_block:creatingProxy
<!--/codeinclude-->

And how to set a simple expectation using the Java MockServerClient.

<!--codeinclude-->
[Setting a simple expectation](../../modules/mockserver/src/test/java/org/testcontainers/mockserver/MockServerContainerTest.java) inside_block:testSimpleExpectation
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-mockserver:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-mockserver</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

Additionally, don't forget to add a [client dependency `org.mock-server:mockserver-client-java`](https://search.maven.org/search?q=mockserver-client-java) 
to be able to set expectations, it's not provided by the testcontainers module. Client version should match to the version in a container tag.
