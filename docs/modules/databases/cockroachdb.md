# CockroachDB Module

Testcontainers module for [CockroachDB](https://hub.docker.com/r/cockroachdb/cockroach)

## Usage example

You can start a CockroachDB container instance from any Java application by using:

<!--codeinclude-->
[Container definition](../../../modules/cockroachdb/src/test/java/org/testcontainers/junit/cockroachdb/SimpleCockroachDBTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:cockroachdb:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>cockroachdb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
