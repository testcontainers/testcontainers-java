# Grafana

Testcontainers module for [Grafana OTel LGTM](https://hub.docker.com/r/grafana/otel-lgtm).

## LGTM's usage examples

You can start a Grafana OTel LGTM container instance from any Java application by using:

<!--codeinclude-->
[Grafana Otel LGTM container](../../modules/grafana/src/test/java/org/testcontainers/grafana/LgtmStackContainerTest.java) inside_block:container
<!--/codeinclude-->

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:grafana:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>grafana</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
