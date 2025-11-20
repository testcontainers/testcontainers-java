# OceanBase Module

Testcontainers module for [OceanBase](https://hub.docker.com/r/oceanbase/oceanbase-ce)

## Usage example

You can start an OceanBase container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/oceanbase/src/test/java/org/testcontainers/oceanbase/SimpleOceanBaseCETest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

### Testcontainers JDBC URL

`jdbc:tc:oceanbasece:4.2.1-lts:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-oceanbase:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-oceanbase</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
