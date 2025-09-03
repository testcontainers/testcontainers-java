# ArcadeDB Module

Testcontainers module for [ArcadeDB](https://hub.docker.com/u/arcadedata)

## Usage example

You can start an ArcadeDB container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/arcadedb/src/test/java/org/testcontainers/containers/ArcadeDBContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:arcadedb:25.3.2"
```
=== "Maven"
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>orientdb</artifactId>
    <version>25.3.2</version>
    <scope>test</scope>
</dependency>
```

!!! hint
Add the following dependencies if you plan to access the Testcontainer:

    === "Gradle"
        ```groovy
        compile "com.arcadedb:arcadedb-engine:25.3.2"
        compile "com.arcadedb:arcadedb-network:25.3.2"
        ```
    
    === "Maven"
        ```xml
        <dependency>
            <groupId>com.arcadedb</groupId>
            <artifactId>arcadedb-engine</artifactId>
            <version>25.7.1</version>
        </dependency>
        <dependency>
            <groupId>com.arcadedb</groupId>
            <artifactId>arcadedb-network</artifactId>
            <version>25.7.1</version>
        </dependency>
        ```
    



