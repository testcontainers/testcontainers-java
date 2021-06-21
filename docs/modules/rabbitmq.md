# RabbitMQ Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

## Usage example

The following example shows how to start a RabbitMQ container.
<!--codeinclude-->
[Create a RabbitMQ container](../../modules/rabbitmq/src/test/java/org/testcontainers/containers/RabbitMQRunningDocuExamplesTest.java) inside_block:createContainer
<!--/codeinclude-->

The following example shows some configuration which is supported by the RabbitMQ module. Check our API for more 
RabbitMQ configuration features.
<!--codeinclude-->
[Create a configured RabbitMQ container](../../modules/rabbitmq/src/test/java/org/testcontainers/containers/RabbitMQRunningDocuExamplesTest.java) inside_block:createConfiguredContainer
<!--/codeinclude-->

The following example shows how to use existing configuration files.
<!--codeinclude-->
[Create a configured RabbitMQ container by using a config file](../../modules/rabbitmq/src/test/java/org/testcontainers/containers/RabbitMQRunningDocuExamplesTest.java) inside_block:createContainerWithConfigFile
<!--/codeinclude-->

The following example shows how to retrieve the connection information.
<!--codeinclude-->
[Get connection information from RabbitMQ container](../../modules/rabbitmq/src/test/java/org/testcontainers/containers/RabbitMQRunningDocuExamplesTest.java) inside_block:getConnectionProperties
<!--/codeinclude-->

The following example shows some SSL configuration.
<!--codeinclude-->
[Use SSL within RabbitMQ container](../../modules/rabbitmq/src/test/java/org/testcontainers/containers/RabbitMQRunningDocuExamplesTest.java) inside_block:sslUsageExample
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:rabbitmq:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>rabbitmq</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
