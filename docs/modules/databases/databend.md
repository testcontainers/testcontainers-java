# Databend Module

Testcontainers module for [Databend](https://hub.docker.com/r/datafuselabs/databend)

## Usage example

You can start a Databend container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/databend/src/test/java/org/testcontainers/databend/DatabendContainerTest.java) inside_block:container
<!--/codeinclude-->

### Testcontainers JDBC URL

`jdbc:tc:databend:v1.2.615:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-databend:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-databend</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

