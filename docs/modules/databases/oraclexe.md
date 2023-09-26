# Oracle-XE Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Usage example

You can use `OracleContainer` like any other JDBC container:
<!--codeinclude-->
[Container creation](../../../modules/oracle-xe/src/test/java/org/testcontainers/junit/oracle/SimpleOracleTest.java) inside_block:constructor
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:oracle-xe:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>oracle-xe</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.


