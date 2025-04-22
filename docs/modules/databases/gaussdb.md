# GaussDB Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Usage example

You can use 'GaussDBContainer' like any other JDBC container:
<!--codeinclude-->
[Container creation](../../../modules/gaussdb/src/test/java/org/testcontainers/junit/gaussdb/SimpleGaussDBTest.java) inside_block:constructor
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:gaussdb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>gaussdb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.


