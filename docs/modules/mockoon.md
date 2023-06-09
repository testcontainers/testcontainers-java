# Mockoon Module

Mockoon can be used to mock HTTP services by using a configuration file with rules and templates.

## Usage example

The following example shows how to start Mockoon.

<!--codeinclude-->
[Creating a Mockoon container](../../modules/mockoon/src/test/java/org/testcontainers/containers/MockoonContainerTest.java) inside_block:initialization
<!--/codeinclude-->

And how to set expectations against the parsed JSON results from Mockoon.

<!--codeinclude-->
[Setting a simple expectation](../../modules/mockoon/src/test/java/org/testcontainers/containers/MockoonContainerTest.java) inside_block:testSimpleExpectation
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle/Kotlin"
    ```kotlin
    testImplementation("org.testcontainers:mockoon:{{latest_version}}")
    ```
=== "Gradle/Groovy"
    ```groovy
    testImplementation "org.testcontainers:mockoon:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>mockoon</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
