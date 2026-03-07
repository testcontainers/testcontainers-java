# Docker Model Runner

This module helps connect to [Docker Model Runner](https://docs.docker.com/desktop/features/model-runner/)
provided by Docker Desktop 4.40.0.

## DockerModelRunner's usage examples

You can start a Docker Model Runner proxy container instance from any Java application by using:

<!--codeinclude-->
[Create a DockerModelRunnerContainer](../../core/src/test/java/org/testcontainers/containers/DockerModelRunnerContainerTest.java) inside_block:container
<!--/codeinclude-->

### Pulling the model

Pulling the model is as simple as:

<!--codeinclude-->
[Pull model](../../core/src/test/java/org/testcontainers/containers/DockerModelRunnerContainerTest.java) inside_block:pullModel
<!--/codeinclude-->

## Adding this module to your project dependencies

*Docker Model Runner support is part of the core Testcontainers library.*

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

