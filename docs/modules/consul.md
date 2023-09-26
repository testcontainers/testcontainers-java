# Hashicorp Consul Module

Testcontainers module for [Consul](https://github.com/hashicorp/consul). Consul is a tool for managing key value properties. More information on Consul [here](https://www.consul.io/).

## Usage example

<!--codeinclude-->
[Running Consul in your Junit tests](../../modules/consul/src/test/java/org/testcontainers/consul/ConsulContainerTest.java)
<!--/codeinclude-->

## Why Consul in Junit tests?

With the increasing popularity of Consul and config externalization, applications are now needing to source properties from Consul.
This can prove challenging in the development phase without a running Consul instance readily on hand. This library 
aims to solve your apps integration testing with Consul. You can also use it to
test how your application behaves with Consul by writing different test scenarios in Junit.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:consul:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>consul</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
