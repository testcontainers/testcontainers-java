# Ollama

Testcontainers module for [Ollama](https://hub.docker.com/r/ollama/ollama) .

## Ollama's usage examples

You can start an Ollama container instance from any Java application by using:

<!--codeinclude-->
[Ollama container](../../modules/ollama/src/test/java/org/testcontainers/ollama/OllamaContainerTest.java) inside_block:container
<!--/codeinclude-->

### Pulling the model

Testcontainers allows [executing commands in the container](../features/commands.md). So, pulling the model is as simple as: 

<!--codeinclude-->
[Pull model](../../modules/ollama/src/test/java/org/testcontainers/ollama/OllamaContainerTest.java) inside_block:pullModel
<!--/codeinclude-->

### Create a new Image

In order to create a new image that contains the model, you can use the following code:

<!--codeinclude-->
[Commit Image](../../modules/ollama/src/test/java/org/testcontainers/ollama/OllamaContainerTest.java) inside_block:commitToImage
<!--/codeinclude-->

And use the new image along with [Image name Substitution](../features/image_name_substitution.md#manual-substitution)

<!--codeinclude-->
[Use new Image](../../modules/ollama/src/test/java/org/testcontainers/ollama/OllamaContainerTest.java) inside_block:substitute
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
