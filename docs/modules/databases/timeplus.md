# Timeplus Module

Testcontainers module for [Timeplus](https://hub.docker.com/r/timeplus/timeplusd)

## Usage example

You can start a Timeplus container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/timeplus/src/test/java/org/testcontainers/timeplus/TimeplusContainerTest.java) inside_block:container
<!--/codeinclude-->

### Testcontainers JDBC URL

`jdbc:tc:timeplus:2.3.21:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-timeplus:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-timeplus</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

