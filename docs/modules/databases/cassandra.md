# Cassandra Module

## Usage example

This example connects to the Cassandra Cluster, creates a keyspaces and asserts that is has been created.

<!--codeinclude-->
[Building CqlSession](../../../modules/cassandra/src/test/java/org/testcontainers/containers/CassandraDriver4Test.java) inside_block:cassandra
<!--/codeinclude-->

!!! warning
    All methods returning instances of the Cassandra Driver's Cluster object in `CassandraContainer` have been deprecated. Providing these methods unnecessarily couples the Container to the Driver and creates potential breaking changes if the driver is updated.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:cassandra:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>cassandra</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
