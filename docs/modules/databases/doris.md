# Apache Doris Module

Testcontainers module for [Apache Doris](https://hub.docker.com/r/apache/doris)

## Usage example

You can start an Apache Doris container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/doris/src/test/java/org/testcontainers/doris/DorisContainerTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

The module uses the separated Apache Doris FE and BE images by default. For example, `apache/doris:3.1.0`
starts `apache/doris:fe-3.1.0` and a managed `apache/doris:be-3.1.0` container. All-in-one image tags
such as `apache/doris:doris-all-in-one-2.1.0` and `apache/doris:3.0.5-all` are still supported when
explicitly requested.

This module starts Doris in storage-compute integrated mode. Doris 3.0+ storage-compute decoupled mode
requires additional services such as Meta Service, FoundationDB, and shared object or file storage, so it
is not exposed by `DorisContainer`.

### Testcontainers JDBC URL

`jdbc:tc:doris:3.1.0:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-doris:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-doris</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
