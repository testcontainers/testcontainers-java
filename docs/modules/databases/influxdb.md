# InfluxDB Module

Testcontainers module for InfluxData [InfluxDB](https://www.influxdata.com/products/influxdb/).

## Important note

There are breaking changes in InfluxDB 2.x.
For more information refer to the main [documentation](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/).
You can find more information about the official InfluxDB image on [Docker Hub](https://hub.docker.com/_/influxdb).

## InfluxDB 2.x usage example

Running a `InfluxDBContainer` as a stand-in for InfluxDB in a test:

<!--codeinclude-->
[Create an InfluxDB container](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerTest.java) inside_block:constructorWithDefaultVariables
<!--/codeinclude-->


The InfluxDB instance will be setup with the following data:<br/>

| Property     | Default Value | 
|--------------|:-------------:|
| username     |   test-user   | 
| password     | test-password | 
| organization |   test-org    |
| bucket       |  test-bucket  |  
| retention    | 0 (infinite)  |
| adminToken   |       -       |

For more details about the InfluxDB setup, please visit the official [InfluxDB documentation](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/docker/#influxdb-2x-initialization-credentials).

It is possible to overwrite the default property values. Create a container with InfluxDB admin token:
<!--codeinclude-->
[Create an InfluxDB container with admin token](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerTest.java) inside_block:constructorWithAdminToken
<!--/codeinclude-->

Or create a container with custom username, password, bucket, organization, and retention time:
<!--codeinclude-->
[Create an InfluxDB container with custom settings](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerTest.java) inside_block:constructorWithCustomVariables
<!--/codeinclude-->

The following code snippet shows how you can create an InfluxDB Java client:

<!--codeinclude-->
[Create an InfluxDB Java client](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerTest.java) inside_block:createInfluxDBClient
<!--/codeinclude-->

!!! hint
    You can find the latest documentation about the InfluxDB 2.x Java client [here](https://github.com/influxdata/influxdb-client-java).

## InfluxDB 1.x usage example

Running a `InfluxDBContainer` as a stand-in for InfluxDB in a test with default env variables:

<!--codeinclude-->
[Create an InfluxDB container](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV1Test.java) inside_block:constructorWithDefaultVariables
<!--/codeinclude-->

The InfluxDB instance will be setup with the following data:<br/>

| Property      | Default Value | 
|---------------|:-------------:|
| username      |   test-user   | 
| password      | test-password | 
| authEnabled   |     true      |  
| admin         |     admin     |
| adminPassword |   password    |
| database      |       -       |

It is possible to overwrite the default values. 
For instance, creating an InfluxDB container with a custom username, password, and database name:
<!--codeinclude-->
[Create an InfluxDB container with custom settings](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV1Test.java) inside_block:constructorWithUserPassword
<!--/codeinclude-->

In the following example you will find a snippet to create an InfluxDB client using the official Java client:

<!--codeinclude-->
[Create an InfluxDB Java client](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV1Test.java) inside_block:createInfluxDBClient
<!--/codeinclude-->

!!! hint
    You can find the latest documentation about the InfluxDB 1.x Java client [here](https://github.com/influxdata/influxdb-java).

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"

```groovy
testImplementation "org.testcontainers:influxdb:{{latest_version}}"
```

=== "Maven"

```xml

<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>influxdb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
