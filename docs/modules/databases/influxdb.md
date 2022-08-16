# InfluxDB Module

Testcontainers module for InfluxData [InfluxDB](https://www.influxdata.com/products/influxdb/).

## Important note

There are breaking changes in InfluxDB 2.x. For more information refer to the
main [documentation](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/). InfluxDB
official [container registry](https://hub.docker.com/_/influxdb) on docker hub.

## InfluxDB 2.x usage example

Running influxDb container as a stand-in for InfluxDB in a test:

<!--codeinclude-->
[InfluxDBContainerTest](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerTest.java)
<!--/codeinclude-->


The influxDB will be setup with the following data:<br/>

| Property     | Default Value | 
|--------------|:-------------:|
| username     |   test-user   | 
| password     | test-password | 
| organization |   test-org    |
| bucket       |  test-bucket  |  
| retention    | 0 (infinite)  |
| adminToken   |       -       |

For more details about the InfluxDB setup go to the
official [InfluxDB docs](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/docker/#influxdb-2x-initialization-credentials)
.

In the following you will find a snippet to create a InfluxDB client using the java client:

```java
public class SomeTest {

    @Rule
    public InfluxDBContainer influxDbContainer = new InfluxDBContainer();

    @Test
    public void someTestMethod() {
        InfluxDBClient influxDB = getInfluxDBClient(this.influxDbContainer);
        // ...
    }
    
    private static InfluxDBClient getInfluxDBClient(final InfluxDBContainer influxDBContainer) {
        final InfluxDBClientOptions influxDBClientOptions = InfluxDBClientOptions
            .builder()
            .url(influxDBContainer.getUrl())
            .authenticate(influxDBContainer.getUsername(), influxDBContainer.getPassword().toCharArray())
            .bucket(influxDBContainer.getBucket())
            .org(influxDBContainer.getOrganization())
            .build();
        return InfluxDBClientFactory.create(influxDBClientOptions);
    }
}
```

!!! hint
    You can find the latest documentation about the InfluxDB 2.x java client [here](https://github.com/influxdata/influxdb-client-java).

## InfluxDB 1.x usage example

Running InfluxDb container as a stand-in for InfluxDB in a test:

<!--codeinclude-->
[InfluxDBContainerV1Test](../../../modules/influxdb/src/test/java/org/testcontainers/containers/InfluxDBContainerV1Test.java)
<!--/codeinclude-->

The influxDB will be setup with the following data:<br/>

| Property      | Default Value | 
|---------------|:-------------:|
| username      |   test-user   | 
| password      | test-password | 
| authEnabled   |     true      |  
| admin         |     admin     |
| adminPassword |   password    |
| database      |       -       |

In the following you will find a snippet to create a InfluxDB client using the java client:

```java
public class SomeTest {

    @Rule
    public InfluxDBContainer influxDbContainer = new InfluxDBContainer();

    @Test
    public void someTestMethod() {
        InfluxDB influxDB = influxDbContainer.getNewInfluxDB();
        // ...
    }
}
```

!!! hint
    You can find the latest documentation about the InfluxDB 1.x java client [here](https://github.com/influxdata/influxdb-java).

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
