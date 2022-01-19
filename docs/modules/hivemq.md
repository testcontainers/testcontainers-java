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


## Using HiveMQ CE/EE

We provide different editions of HiveMQ on [Docker-Hub](https://hub.docker.com/u/hivemq):

- the open source [Community Edition](https://github.com/hivemq/hivemq-community-edition) which 
is tagged as *hivemq/hivemq-ce*.
- the Enterprise Edition which is tagged as *hivemq/hivemq-ee*.

Both edition can be used directly as testcontainers:

Using the Community Edition:
<!--codeinclude-->
[Community Edition HiveMQ-Testcontainer](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoHiveMQContainerIT.java) inside_block:ceVersion
<!--/codeinclude-->

Using the Enterprise Edition:
<!--codeinclude-->
[Enterprise Edition HiveMQ-Testcontainer](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoHiveMQContainerIT.java) inside_block:eeVersion
<!--/codeinclude-->

Using a specifc version is possible by using the tag:
<!--codeinclude-->
[Specific Version HiveMQ-Testcontainer](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoHiveMQContainerIT.java) inside_block:specificVersion
<!--/codeinclude-->

## Test your MQTT 3 and MQTT 5 client application

Using an Mqtt-client (e.g. the [HiveMQ-Mqtt-Client](https://github.com/hivemq/hivemq-mqtt-client)) you can start 
testing directly. 

<!--codeinclude-->
[MQTT5 Client](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoHiveMQContainerIT.java) inside_block:mqtt5client
<!--/codeinclude-->

## Settings

There are several things that can be adjusted before container setup.
The following example shows how to enable the Control Center (this is an enterprise feature), set the log level to DEBUG
(`.silent(true)` can be used to turn off all output) and load a HiveMQ-config-file from the classpath.
The contents of *config.xml* are documented [here](https://github.com/hivemq/hivemq-community-edition/wiki/Configuration).

---
**Note:**
After startup, you are presented with the URL of the HiveMQ Control Center:

```
2021-09-10 10:35:53,511 INFO  - The HiveMQ Control Center is reachable under: http://localhost:55032
```

<!--codeinclude-->
[Config Examples](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoContainerConfigIT.java) inside_block:containerConfig
<!--/codeinclude-->

---

## Configure Docker resources

It might be required to adjust docker resources (CPU/RAM/...).
To do so we provide a way to modify the HostConfig:

<!--codeinclude-->
[Docker resource definitons](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoContainerConfigIT.java) inside_block:dockerConfig
<!--/codeinclude-->

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

<!--codeinclude-->
[Custom Wait Strategy](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoExtensionTestsIT.java) inside_block:waitStrategy
<!--/codeinclude-->

Next up we have an example for using an extension directly from the classpath and waiting directly on the extension:

<!--codeinclude-->
[Extension from Classpath](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoExtensionTestsIT.java) inside_block:extensionClasspath
<!--/codeinclude-->

---
**Note** Debugging extensions

Both examples contain ```.withDebugging()``` which enables remote debugging on the container.
With debugging enabled you can start putting breakpoints right into your extensions.

--- 

### Testing extensions using Gradle

In a Gradle based HiveMQ Extension project we support testing using a dedicated [HiveMQ Extension Gradle Plugin](https://github.com/hivemq/hivemq-extension-gradle-plugin/README.md).

The plugin adds an `integrationTest` task which executes tests from the `integrationTest` source set.
- Integration test source files are defined in `src/integrationTest`.
- Integration test dependencies are defined via the `integrationTestImplementation`, `integrationTestRuntimeOnly`, etc. configurations.

The `integrationTest` task builds the extension and unzips it to the `build/hivemq-extension-test` directory.
The tests can then load the built extension into the HiveMQ Testcontainer.

```java
@Container
final HiveMQContainer hivemq = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
    .withExtension(new File("build/hivemq-extension-test/<extension-id>"));
```

### Enable/Disable an extension

It is possible to enable and disable HiveMQ extensions during runtime. Extensions can also be disabled on startup.

---
**Note**: that disabling or enabling of extension during runtime is only supported in HiveMQ 4 Enterprise Edition Containers.

---

The following example shows how to start a HiveMQ-testcontainer with the extension called **my-extension** being disabled.


<!--codeinclude-->
[Disable Extension at startup](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoDisableExtensionsIT.java) inside_block:startDisabled
<!--/codeinclude-->

The following test then proceeds to enable and then disable the extension:

<!--codeinclude-->
[Enable/Disable extension at runtime](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoDisableExtensionsIT.java) inside_block:startDisabled
<!--/codeinclude-->

## Enable/Disable an extension loaded from a folder

Extensions loaded from an extension folder during runtime can also be enabled/disabled on the fly.
If the extension folder contains a DISABLED file, the extension will be disabled during startup.

---
**Note**: that disabling or enabling of extension during runtime is only supported in HiveMQ 4 Enterprise Edition Containers.

---

We first load the extension from the filesytem:
<!--codeinclude-->
[Extension from filesystem](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoDisableExtensionsIT.java) inside_block:startFromFilesystem
<!--/codeinclude-->

Now we can enable/disable the extension using its name:

<!--codeinclude-->
[Enable/Disable extension at runtime](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoDisableExtensionsIT.java) inside_block:runtimeEnableFilesystem
<!--/codeinclude-->

### Remove prepackaged HiveMQ Extensions

Since HiveMQ's 4.4 release, HiveMQ Docker images come with the HiveMQ Extension for Kafka, the HiveMQ Enterprise Bridge Extension
and the HiveMQ Enterprise Security Extension.
These Extensions are disabled by default, but sometimes you my need to remove them before the container starts.

Removing all extension is as simple as:

<!--codeinclude-->
[Remove all extensions](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoDisableExtensionsIT.java) inside_block:noExtensions
<!--/codeinclude-->

A single extension (e.g. Kafka) can be removed as easily:
<!--codeinclude-->
[Remove a specific extension](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoDisableExtensionsIT.java) inside_block:noKafkaExtension
<!--/codeinclude-->

## Put files into the container

### Put a file into HiveMQ home

<!--codeinclude-->
[Put file into HiveMQ home](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoFilesIT.java) inside_block:hivemqHome
<!--/codeinclude-->

### Put files into extension home

<!--codeinclude-->
[Put file into HiveMQ-Extension home](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoFilesIT.java) inside_block:extensionHome
<!--/codeinclude-->

### Put license files into the container

<!--codeinclude-->
[Put license file into HiveMQ-testcontainer](../../modules/hivemq/src/test/java/org/testcontainers/hivemq/docs/DemoFilesIT.java) inside_block:withLicenses
<!--/codeinclude-->



### Customize the Container further

Since the `HiveMQContainer` extends from [Testcontainer's](https://github.com/testcontainers) `GenericContainer` the container
can be customized as desired.

## Contributing

If you want to contribute to the HiveMQ Testcontainer, see the [contribution guidelines](CONTRIBUTING.md).
