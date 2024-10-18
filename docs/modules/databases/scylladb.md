# ScyllaDB Module

## Usage example

This example connects to a ScyllaDB Cluster, creates a keyspace and asserts that is has been created.

<!--codeinclude-->
[Building CqlSession](../../../modules/scylladb/src/test/java/org/testcontainers/containers/ScyllaDBDriver4Test.java) inside_block:scylladb
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:scylladb:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>scylladb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
