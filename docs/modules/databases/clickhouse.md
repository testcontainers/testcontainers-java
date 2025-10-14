# Clickhouse Module

Testcontainers module for [ClickHouse](https://hub.docker.com/r/clickhouse/clickhouse-server)

## Usage example

You can start a ClickHouse container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/clickhouse/src/test/java/org/testcontainers/clickhouse/ClickHouseContainerTest.java) inside_block:container
<!--/codeinclude-->

### Testcontainers JDBC URL

`jdbc:tc:clickhouse:18.10.3:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-clickhouse:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-clickhouse</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

