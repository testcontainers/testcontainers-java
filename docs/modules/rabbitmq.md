# RabbitMQ Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

## Usage example

The following example shows how to start a RabbitMQ container.
```java
class RabbitMQIntegrationTest {

    @ClassRule
    private static final RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.8.9");
    
    @Test
    public void someTestMethod() {
        // use already started container here
    }
}
```

The following example shows some configuration which is supported by the RabbitMQ module. Check our API for more 
RabbitMQ configuration features.
```java
class RabbitMQIntegrationTest {

    @ClassRule
    private static final RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.8.9")
        .withUser("Username", "Password")
        .withQueue("test-queue")
        .withExchange("test.exchange", "topic")
        .withBinding("test.exchange", "test-queue", new HashMap<>(), "routing.key.foo.bar", "queue");
    
    @Test
    public void someTestMethod() {
        // use already started and configured container here
    }
}
```

The following example shows how to use existing configuration files.
```java
class RabbitMQIntegrationTest {
    
    @ClassRule
    private static final RabbitMQContainer containerWithRabbitMQConfig = new RabbitMQContainer("rabbitmq:3.8.9")
        .withRabbitMQConfig(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
    
    // or
    
    @ClassRule
    private static final RabbitMQContainer containerWithSysctlConfig = new RabbitMQContainer("rabbitmq:3.8.9")
        .withRabbitMQConfigSysctl(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
    
    // or
    
    @ClassRule
    private static final RabbitMQContainer containerWithErlangConfig = new RabbitMQContainer("rabbitmq:3.8.9")
        .withRabbitMQConfigErlang(MountableFile.forClasspathResource("/rabbitmq-custom.config"));
    
    @Test
    public void someTestMethod() {
        // use already started and configured container here
    }
}
```

The following example shows how to retrieve the connection information.
```java
class RabbitMQIntegrationTest {

    @ClassRule
    private static final RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.8.9-management");
    
    @Test
    public void someTestMethod() {
        final String amqpUrl = container.getAmqpUrl(); // will provide the connection string 'amqp://<container-ip>:<mapped-amqp-port>'
        final String managementUrl = container.getHttpUrl(); // will provide the connection string 'http://<container-ip>:<mapped-management-port>'
        final String adminUsername = container.getAdminUsername();
        final String adminPassword = container.getAdminPassword();
        // setup your connection here
    }
}
```

The following example shows some SSL configuration.
```java
class RabbitMQIntegrationTest {

    @ClassRule
    private static final RabbitMQContainer container = new RabbitMQContainer("rabbitmq:3.8.9-management")
        .withSSL(
            MountableFile.forClasspathResource("/certs/server_key.pem", 0644),
            MountableFile.forClasspathResource("/certs/server_certificate.pem", 0644),
            MountableFile.forClasspathResource("/certs/ca_certificate.pem", 0644),
            SslVerification.VERIFY_PEER,
            true
        );
    
    @Test
    public void someTestMethod() {
        final String superSecureAmqpsUrl = container.getAmqpsUrl();
        final String superSecureManagementUrl = container.getHttpsUrl();
        // setup your super secure connections here
    }
}
```

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
