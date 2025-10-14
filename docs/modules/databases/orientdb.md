# OrientDB Module

Testcontainers module for [OrientDB](https://hub.docker.com/_/orientdb/)

## Usage example

You can start an OrientDB container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/orientdb/src/test/java/org/testcontainers/orientdb/OrientDBContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-orientdb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-orientdb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Add the OrientDB Java client if you plan to access the Testcontainer:
    
    === "Gradle"
        ```groovy
        compile "com.orientechnologies:orientdb-client:3.0.24"
        ```
    
    === "Maven"
        ```xml
        <dependency>
            <groupId>com.orientechnologies</groupId>
            <artifactId>orientdb-client</artifactId>
            <version>3.0.24</version>
        </dependency>
        ```
    



