# Milvus

Testcontainers module for [Milvus](https://hub.docker.com/r/milvusdb/milvus).

## Milvus's usage examples

You can start a Milvus container instance from any Java application by using:

<!--codeinclude-->
[Default config](../../modules/milvus/src/test/java/org/testcontainers/mivul/MilvusContainerTest.java) inside_block:milvusContainer
<!--/codeinclude-->

With external Etcd:

<!--codeinclude-->
[External Etcd](../../modules/milvus/src/test/java/org/testcontainers/milvus/MilvusContainerTest.java) inside_block:externalEtcd
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:milvus:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>milvus</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
