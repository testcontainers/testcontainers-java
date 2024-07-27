# ChromaDB

Testcontainers module for [ChromaDB](https://registry.hub.docker.com/r/chromadb/chroma)

## ChromaDB's usage examples

You can start a ChromaDB container instance from any Java application by using:

<!--codeinclude-->
[Default ChromaDB container](../../modules/chromadb/src/test/java/org/testcontainers/chromadb/ChromaDBContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:chromadb:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
<groupId>org.testcontainers</groupId>
<artifactId>chromadb</artifactId>
<version>{{latest_version}}</version>
<scope>test</scope>
</dependency>
```
