# Pinecone

Testcontainers module for [Pinecone Local](https://github.com/orgs/pinecone-io/packages/container/package/pinecone-local).

## PineconeLocalContainer's usage examples

You can start a Pinecone container instance from any Java application by using:

<!--codeinclude-->
[Pinecone container](../../modules/pinecone/src/test/java/org/testcontainers/pinecone/PineconeLocalContainerTest.java) inside_block:container
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
