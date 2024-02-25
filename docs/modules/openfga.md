# OpenFGA

Testcontainers module for [OpenFGA](https://hub.docker.com/r/"openfga/openfga).

## OpenFGAContainer's usage examples

You can start an OpenFGA container instance from any Java application by using:

<!--codeinclude-->
[OpenFGA container](../../modules/openfga/src/test/java/org/testcontainers/openfga/OpenFGAContainerTest.java) inside_block:container
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:openfga:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>openfga</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
