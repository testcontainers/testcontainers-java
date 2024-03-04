# Ollama

Testcontainers module for [Ollama](https://hub.docker.com/r/ollama/ollama) .

## Ollama's usage examples

You can start an Ollama container instance from any Java application by using:

<!--codeinclude-->
[Ollama container](../../modules/ollama/src/test/java/org/testcontainers/ollama/OllamaContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:ollama:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>ollama</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
