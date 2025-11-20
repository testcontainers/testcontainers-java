# Oracle Database Free Module

Testcontainers module for [Oracle Free](https://hub.docker.com/r/gvenzl/oracle-free)

## Usage example

You can start an Oracle-Free container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/oracle-free/src/test/java/org/testcontainers/junit/oracle/SimpleOracleTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

### Testcontainers JDBC URL

`jdbc:tc:oracle:21-slim-faststart:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-oracle-free:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-oracle-free</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.


