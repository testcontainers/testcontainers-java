# Cassandra Module

## Usage example

This example connects to the Cassandra cluster:

1. Define a container:
    <!--codeinclude-->
    [Container definition](../../../modules/cassandra/src/test/java/org/testcontainers/cassandra/CassandraDriver4Test.java) inside_block:container-definition
    <!--/codeinclude-->

2. Build a `CqlSession`:
    <!--codeinclude-->
    [Building CqlSession](../../../modules/cassandra/src/test/java/org/testcontainers/cassandra/CassandraDriver4Test.java) inside_block:cql-session
    <!--/codeinclude-->

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
