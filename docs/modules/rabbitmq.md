# RabbitMQ Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

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

## Example

<!--codeinclude-->
[Default RabbitMQ container with no customization](../examples/src/test/java/rabbitmq/RabbitMQSimpleTest.java) inside_block:simple
<!--/codeinclude-->

## Declaring Objects

The `RabbitMQContainer` can be configured to declare the following types of objects on startup:

* binding
* exchange
* operator policy
* parameter
* permission
* policy
* queue
* user
* vhost
* vhost limit

To declare objects, call the `RabbitMQContainer`'s `declare(...)` method,
and pass an object returned from one of the static factory methods in `DeclareCommands`.
Each of the static factory methods returns an object that can be further customized as needed.
Consider statically importing the methods in `DeclareCommands` for more fluent code.

<!--codeinclude-->
[Declared Objects](../examples/src/test/java/rabbitmq/RabbitMQDeclareTest.java) inside_block:declares
<!--/codeinclude-->

<!--codeinclude-->
[Static Imports](../examples/src/test/java/rabbitmq/RabbitMQDeclareTest.java) inside_block:imports
<!--/codeinclude-->

As seen in the above example, to declare objects _inside a vhost_, use the `declare(...)` method
on the object returned from `vhost(...)`, rather than the `RabbitMQContainer`'s `declare(...)` method.
Objects not explicitly declared in a vhost will be declared in the default vhost (`/`).

Objects will be declared in the order in which the declare methods are called.
Order is important for objects that depend on each other.
For example, permissions for a user must be declared after the user.

Objects are declared using the `rabbitmqadmin` CLI command, which must be available in the container.
Therefore, it is recommended to use the `*management*` tagged rabbitmq images (e.g. `3.7-management-alpine`).

## Enabling Plugins

RabbitMQ plugins can be enabled via `withPluginsEnabled(...)`.

<!--codeinclude-->
[Some plugins enabled](../examples/src/test/java/rabbitmq/RabbitMQPluginsTest.java) inside_block:plugins
<!--/codeinclude-->

## Admin User

The admin username is `guest` and cannot be changed, but can be retrieved via `getAdminUsername()`.

The admin password by default is `guest`, and can be changed via `withAdminPassword(...)`,
and retrieved via `getAdminPassword()`.

<!--codeinclude-->
[Specific admin password](../examples/src/test/java/rabbitmq/RabbitMQAdminUserTest.java) inside_block:RabbitMQAdminUserTest
<!--/codeinclude-->

## SSL

SSL can be enabled via one of the `withSSL(...)` methods.

<!--codeinclude-->
[SSL enabled](../examples/src/test/java/rabbitmq/RabbitMQSslTest.java) inside_block:ssl
<!--/codeinclude-->

## Config file

RabbitMQ's default configuration file can be overridden via one of the following methods:

* `withRabbitMQConfig` - sysctl format  (RabbitMQ version >= 3.7)
* `withRabbitMQConfigSysctl` - sysctl format  (RabbitMQ version >= 3.7)
* `withRabbitMQConfigErlang` - erlang format

<!--codeinclude-->
[Custom sysctl config file](../examples/src/test/java/rabbitmq/RabbitMQConfigTest.java) inside_block:config
<!--/codeinclude-->

## Connecting

Use the following methods to get connection details to the RabbitMQ container:

* `getAmqpUrl()` (or `getAmqpsUrl()` if [ssl](#ssl) is enabled) - to get the full url 
* `getAmqpPort()` (or `getAmqpsPort()` if [ssl](#ssl) is enabled)- to get only the port
* `getContainerIpAddress()` - to get only the ip address

<!--codeinclude-->
[Non-SSL](../examples/src/test/java/rabbitmq/RabbitMQSimpleTest.java) inside_block:connection
<!--/codeinclude-->

<!--codeinclude-->
[SSL](../examples/src/test/java/rabbitmq/RabbitMQSslTest.java) inside_block:connection
<!--/codeinclude-->
