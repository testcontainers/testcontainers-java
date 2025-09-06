# Hashicorp Nacos Module

Testcontainers module for [Nacos](https://github.com/alibaba/nacos). Nacos an easy-to-use dynamic service discovery, configuration and service management platform for building AI cloud native applications. More information on Nacos [here](https://nacos.io/docs/latest/overview/).

## Usage example

<!--codeinclude-->
[Running Nacos in your Junit tests](../../modules/nacos/src/test/java/org/testcontainers/nacos/NacosContainerTest.java)
<!--/codeinclude-->

## Why Nacos in Junit tests?

With the increasing popularity of Nacos and config externalization, applications are now needing to source properties from Nacos.
This can prove challenging in the development phase without a running Nacos instance readily on hand. This library 
aims to solve your apps integration testing with Nacos. You can also use it to
test how your application behaves with Nacos by writing different test scenarios in Junit.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:nacos:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>nacos</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
