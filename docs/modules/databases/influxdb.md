# InfluxDB Module

Testcontainers module for InfluxData [InfluxDB](https://www.influxdata.com/products/influxdb/).

## Important Note

They are breaking changes in InfluxDB v2.x. For more information refer to the
main [documentation](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/). InfluxDB
official [container registry](https://hub.docker.com/_/influxdb) on docker hub.

## InfluxDB V2.x Usage example

Running influxDbContainer as a stand-in for InfluxDB in a test:

<!--codeinclude-->
[InfluxDBContainerV2Test](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV2Test.java)
<!--/codeinclude-->


The influxDB will be setup with the following data:<br/>

| Property      | Default Value | 
| ------------- |:-------------:|
| username      | test-user     | 
| password      | test-password | 
| organization  | test-org      |
| bucket        | test-bucket   |  
| retention     | 0 (infinite)  |
| adminToken    |       -       |

For more details about the InfluxDB setup go to the
official [docs](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/docker/#influxdb-2x-initialization-credentials)
.

It is possible to override the default values, take a look at these tests:

<!--codeinclude-->
[InfluxDBContainerV2WithUserTest](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV2WithUserTest.java)
<!--/codeinclude-->

**NOTE**: You can find the latest documentation about the influxdb v2.x java
client [here](https://github.com/influxdata/influxdb-client-java).

## InfluxDB V1.x Usage example (Deprecated)

Running influxDbContainer as a stand-in for InfluxDB in a test:

<!--codeinclude-->
[InfluxDBContainerV1Test](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV1Test.java)
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:influxdb:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>influxdb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

**Hint:** Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You
should ensure that your project also has a suitable database driver as a dependency.
