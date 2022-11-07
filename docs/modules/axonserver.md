# Axon Server Containers

Testcontainers can be used to automatically instantiate and manage both [Axon Server SE](https://axoniq.io/product-overview/axon-server) and [Axon Server EE](https://axoniq.io/product-overview/axon-server-enterprise) containers.
It uses the official [docker images](https://hub.docker.com/u/axoniq) provided by AxonIQ.

## Benefits

* Running a single node Axon Server SE/EE with just one line of code

## Example

### Axon Server Standard Edition (SE)

Create an `AxonServerSEContainer` to use in your tests.

```java
final AxonServerSEContainer axonServerSEContainer =
                new AxonServerSEContainer(DockerImageName.parse("axoniq/axonserver:4.4.12"));
```

This version is the simplest one and also included some utils methods to get the Axon Server Address. The only out of the box configuration provided is the `devMode` flag:
* `withDevMode` where you can specify if you want dev-mode to be enabled or not. Default is `false`

### Axon Server Enterprise Edition (EE)

Create an `AxonServerEEContainer` to use in your tests.

```java
final AxonServerEEContainer axonServerEEContainer =
    new AxonServerEEContainer(DockerImageName.parse("axoniq/axonserver-enterprise:4.5.9-dev"));
```

This version is more complex and provides additional configuration listed below:
* `withLicense` where you can provide a path to your license file
* `withAutoCluster` where you can provide a path to your auto-cluster file
* `withConfiguration` where you can provide a path to your `axonserver.properties` file
* `withAxonServerName` where you can provide Axon Server's name
* `withAxonServerHostname` where you can provide Axon Server's hostname
* `withAxonServerInternalHostname` where you can provide Axon Server's internal hostname

It also includes some utils methods to get the Axon Server Address. 

### Configuration

For an extensive list of environment variables you can use, please check the [official docs](https://docs.axoniq.io/reference-guide/v/master/axon-server/administration/admin-configuration/configuration#configuration-properties).

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:axonserver:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>axonserver</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
