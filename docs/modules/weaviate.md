# Weaviate

Testcontainers module for [Weaviate](https://hub.docker.com/r/semitechnologies/weaviate)

## WeaviateContainer's usage examples

You can start an Weaviate container instance from any Java application by using:

<!--codeinclude-->
[Default Weaviate container](../../modules/weaviate/src/test/java/org/testcontainers/weaviate/WeaviateContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:weaviate:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
<groupId>org.testcontainers</groupId>
<artifactId>weaviate</artifactId>
<version>{{latest_version}}</version>
<scope>test</scope>
</dependency>
```
