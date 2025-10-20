# Trino Module

Testcontainers module for [Trino](https://hub.docker.com/r/trinodb/trino)

## Usage example

You can start a Trino container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/trino/src/test/java/org/testcontainers/trino/TrinoContainerTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all database container types.

### Testcontainers JDBC URL

`jdbc:tc:trino:352:///defaultname`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-trino:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-trino</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add the Trino JDBC driver JAR to your project.
    You should ensure that your project has the Trino JDBC driver as a dependency, if you plan on using it.
    Refer to [Trino project download page](https://trino.io/download.html) for instructions.


