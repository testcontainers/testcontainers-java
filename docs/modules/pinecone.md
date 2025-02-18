# Pinecone

Testcontainers module for [Pinecone](https://github.com/orgs/pinecone-io/packages/container/package/pinecone-local).

## PineconeContainer's usage examples

You can start an Pinecone container instance from any Java application by using:

<!--codeinclude-->
[Pinecone container](../../modules/pinecone/src/test/java/org/testcontainers/pinecone/PineconeContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:pinecone:{{latest_version}}"
```

=== "Maven"
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>pinecone</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
