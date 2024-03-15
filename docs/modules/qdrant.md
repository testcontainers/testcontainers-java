# Qdrant

Testcontainers module for [Qdrant](https://registry.hub.docker.com/r/qdrant/qdrant)

## Qdrant's usage examples

You can start a Qdrant container instance from any Java application by using:

<!--codeinclude-->
[Default QDrant container](../../modules/qdrant/src/test/java/org/testcontainers/qdrant/QdrantContainerTest.java) inside_block:qdrantContainer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:qdrant:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>qdrant</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
