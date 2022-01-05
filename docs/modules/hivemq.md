# HiveMQ Module

![image](https://www.hivemq.com/img/logo-hivemq-testcontainer.png)

Automatic starting HiveMQ docker containers for JUnit4 and JUnit5 tests.
This enables testing MQTT client applications and integration testing of custom HiveMQ extensions.

- Community forum: https://community.hivemq.com/
- HiveMQ website: https://www.hivemq.com/
- MQTT resources:
    - [MQTT Essentials](https://www.hivemq.com/mqtt-essentials/)
    - [MQTT 5 Essentials](https://www.hivemq.com/mqtt-5/)

## Add to your project

### Gradle

Add to `build.gradle`:

````groovy
testImplementation 'org.testcontainers:hivemq:{{latest_version}}'
````

Add to `build.gradle.kts`:

````kotlin
testImplementation("org.testcontainers:hivemq:{{latest_version}}")
````

### Maven

Add to `pom.xml`:

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>hivemq</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```


## User defined HiveMQ image and tag

The default image is the 'hivemq/hivemq-ce' using the '2021.3' version.
As always, the constructor allows to specify a custom image and tag, details on the available tags for community edition
are available in our [Docker-repository](https://hub.docker.com/r/hivemq/hivemq-ce).

You can alos pick the HiveMQ-enterprise edition using the image name 'hivemq/hivemq4'. Please check the [Docker-repository](https://hub.docker.com/r/hivemq/hivemq4)
 for available tags.

An example for explicitly specifiying image and version.
```java    
import org.testcontainers.hivemq.HiveMQContainer

public class MqttTest {

     @Container
     final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:2021.3"));
     
}

## Test your MQTT 3 and MQTT 5 client application

Using an Mqtt-client (e.g. the [HiveMQ-Mqtt-Client](https://github.com/hivemq/hivemq-mqtt-client)) you can start 
testing directly. 

```java
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;

public class MqttTest {

@Container
final HiveMQContainer hivemq = new HiveMQContainer()

@Test
public void test_mqtt() {
    
    final Mqtt5BlockingClient client = Mqtt5Client.builder()
        .serverPort(hivemq.getMqttPort())
        .buildBlocking();

    client.connect();
    client.disconnect();
}
}
```

## Set logging level

The logging level of the HiveMQ testcontainer can be controlled using the withLogLevel-methid on the builder.

---
**Note:** you can silence the container at any time using the `.silent(true)` method.

---

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withLogLevel(Level.DEBUG);
```

## HiveMQ Control Center

The HiveMQ Testcontainer can make the HiveMQ Control Center available.

---
**Note:** that the HiveMQ Control Center is a feature of the HiveMQ Enterprise Edition.

---

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withControlCenter();
```

After startup, you are presented with the URL of the HiveMQ Control Center:

```
2021-09-10 10:35:53,511 INFO  - The HiveMQ Control Center is reachable under: http://localhost:55032
```

## Add a custom HiveMQ configuration

In situations where additional configuration is required the file can be specified using the provided builder.
The contents of *config.xml* are documented [here](https://github.com/hivemq/hivemq-community-edition/wiki/Configuration). 

```java
@RegisterExtension
final HiveMQContainer hivemq = new HiveMQContainer()
        .withHiveMQConfig(MountableFile.forHostPath("/path/to/config.xml"));
```

## Testing HiveMQ extensions

Using the [Extension SDK](https://github.com/hivemq/hivemq-extension-sdk) the functionality of all editions of HiveMQ
can be extended. 
The HiveMQ-testcontainer also supports testing these custom extensions of yours.

### Wait Strategy

The raw HiveMQ-testcontainer is built to wait for certain startup log messages to signal readiness.
Since extensions are loaded dynamically they can be available a short while after the main container has started.
We therefore provide custom wait conditions for HiveMQ Extensions:

The following will specify an extension to be loaded from **src/test/resources/modifier-extension** into the container and 
wait for an  extension named **'My Extension Name'** to be started: 
```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
            .withExtension(MountableFile.forHostPath("src/test/resources/modifier-extension"))
            .waitForExtension("My Extension Name");
```

Next up we have an example for using an extension directly from the classpath:
```java
final HiveMQExtension hiveMQExtension = HiveMQExtension.builder()
    .id("extension-1")
    .name("my-extension")
    .version("1.0")
    .mainClass(MyExtension.class).build();

@Container
final HiveMQContainer hivemq = new HiveMQContainer("hivemq/hivemq4", "latest")
        .withExtension(hiveMQExtension)
        .waitForExtension(hiveMQExtension);
```

### Testing extensions using Gradle

In a Gradle based HiveMQ Extension project we support testing using a dedicated [HiveMQ Extension Gradle Plugin](https://github.com/hivemq/hivemq-extension-gradle-plugin/edit/master/README.md).

The plugin adds an `integrationTest` task which executes tests from the `integrationTest` source set.
- Integration test source files are defined in `src/integrationTest`.
- Integration test dependencies are defined via the `integrationTestImplementation`, `integrationTestRuntimeOnly`, etc. configurations.

The `integrationTest` task builds the extension and unzips it to the `build/hivemq-extension-test` directory.
The tests can then load the built extension into the HiveMQ Testcontainer:
```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer("hivemq/hivemq4", "latest")
    .withExtension(new File("build/hivemq-extension-test/<extension-id>"));
```

### Remote debugging of loaded extensions

You can debug extensions that are directly loaded from your code:

- put a break point in your extension
- enable remote debugging on your container

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withDebugging();
```

#### Load an extension from a Gradle project

It is also possible to reference an extension in a different Gradle project by using the **GradleHiveMQExtensionSupplier**:

```java
import org.testcontainers.hivemq.GradleHiveMQExtensionSupplier;

@Container
final HiveMQContainer hivemq = new HiveMQContainer("hivemq/hivemq4", "latest")
        .withExtension(new GradleHiveMQExtensionSupplier(new File("path/to/extension/")).get());
```

### Testing extensions using Maven

You can package and load an extension from a separate maven project by referencing the **pom.xml** of said project:
```java
import org.testcontainers.hivemq.MavenHiveMQExtensionSupplier;

@Container
final HiveMQContainer hivemq = new HiveMQContainer("hivemq/hivemq4", "latest")
        .withExtension(new MavenHiveMQExtensionSupplier("path/to/extension/pom.xml").get());
```

### Enable/Disable an extension

It is possible to enable and disable HiveMQ extensions during runtime. Extensions can also be disabled on startup.

---
**Note**: that disabling or enabling of extension during runtime is only supported in HiveMQ 4 Enterprise Edition Containers.

---

```java
public class MqttTest {
private final @NotNull HiveMQExtension hiveMQExtension = HiveMQExtension.builder()
    .id("extension-1")
    .name("my-extension")
    .version("1.0")
    .disabledOnStartup(true)
    .mainClass(MyExtension.class).build();

@Container
final HiveMQContainer hivemq = new HiveMQContainer("hivemq/hivemq4", "latest")
        .withExtension(hiveMQExtension);

@Test
void test_disable_enable_extension() throws ExecutionException, InterruptedException {
    hivemq.enableExtension(hiveMQExtension);
    hivemq.disableExtension(hiveMQExtension);
}
}
```

## Enable/Disable an extension loaded from a folder

You can enable or disable extensions loaded from an extension folder during runtime.
If the extension folder contains a DISABLED file, the extension will be disabled during startup.

---
**Note**: that disabling or enabling of extension during runtime is only supported in HiveMQ 4 Enterprise Edition Containers.

---

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer("hivemq/hivemq4", "latest")
            .withExtension(new MountableFile.forHostPath("src/test/resources/modifier-extension"));
            
@Test
void test_disable_enable_extension() throws ExecutionException, InterruptedException {
    hivemq.disableExtension("Modifier Extension", "modifier-extension");
    hivemq.enableExtension("Modifier Extension", "modifier-extension");
}
```

### Remove prepackaged HiveMQ Extensions

Since HiveMQ's 4.4 release, HiveMQ Docker images come with the HiveMQ Extension for Kafka, the HiveMQ Enterprise Bridge Extension
and the HiveMQ Enterprise Security Extension.
These Extensions are disabled by default, but sometimes you my need to remove them before the container starts.

### Remove all prepackaged extensions:

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
    .withoutPrepackagedExtensions();
```

### Remove specific prepackaged extensions:

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
    .withoutPrepackagedExtensions("hivemq-kafka-extension");
```

## Put files into the container

### Put a file into HiveMQ home

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withFileInHomeFolder(
            MountableFile.forHostPath("src/test/resources/additionalFile.txt"),
            "/path/in/home/folder");
```

### Put files into extension home

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withExtension(HiveMQExtension.builder()
            .id("extension-1")
            .name("my-extension")
            .version("1.0")
            .mainClass(MyExtension.class).build())
        .withFileInExtensionHomeFolder(
            MountableFile.forHostPath("src/test/resources/additionalFile.txt"),
            "extension-1",
            "/path/in/extension/home");
```

### Put license files into the container

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withLicense(new File("src/test/resources/myLicense.lic"))
        .withLicense(new File("src/test/resources/myExtensionLicense.elic"));
```

### Configure Docker resources

```java
@RegisterExtension
@Container
final HiveMQContainer hivemq = new HiveMQContainer()
        .withCreateContainerCmdModifier(createContainerCmd -> {
            final HostConfig hostConfig = HostConfig.newHostConfig();
            hostConfig.withCpuCount(2L);
            hostConfig.withMemory(2 * 1024 * 1024L);
        });
```

### Customize the Container further

Since the `HiveMQContainer` extends from [Testcontainer's](https://github.com/testcontainers) `GenericContainer` the container
can be customized as desired.

## Contributing

If you want to contribute to the HiveMQ Testcontainer, see the [contribution guidelines](CONTRIBUTING.md).
