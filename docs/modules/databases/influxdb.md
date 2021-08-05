# InfluxDB Module

Testcontainers module for InfluxData [InfluxDB](https://www.influxdata.com/products/influxdb/).

## Important Note

They are breaking changes in InfluxDB v2.x. For more information refer to the
main [documentation](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/). InfluxDB
official [container registry](https://hub.docker.com/_/influxdb) on docker hub.

## InfluxDB V2.x Usage example

Running influxDbContainer as a stand-in for InfluxDB in a test:

```java
public class SomeTest {

    @ClassRule
    public static final InfluxDBV2Container influxDbContainer =
        new InfluxDBContainerV2<>(DockerImageName.parse("influxdb"));

    @Test
    public void someTestMethod() {
        final InfluxDBClient influxDB = influxDbContainer.getInfluxDBClient();
        // Rest of the test
    }
}
```

This will create a InfluxDB container with the latest tag (
available [tags](https://hub.docker.com/_/influxdb?tab=tags&page=1&ordering=last_updated)). The influxDB will be setup
with the following data:<br/>

| Property      | Default Value | 
| ------------- |:-------------:|
| Username      | test-user     | 
| Password      | test-password | 
| Organization  | test-org      |
| Bucket        | test-bucket   |  
| Retention     | 0 (infinite)  |
| Admin Token   |       -       |

For more details about the InfluxDB setup go to the
official [docs](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/docker/#influxdb-2x-initialization-credentials)
.

It is possible to override the default values and start the container with a newer tag:

```java
public class SomeTest {
    @ClassRule
    public static final InfluxDBContainerV2<?> influxDBContainer =
        new InfluxDBContainerV2<>(InfluxDBV2TestImages.INFLUXDB_TEST_IMAGE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withOrganization(ORG)
            .withBucket(BUCKET)
            .withRetention(RETENTION) // Optional
            .withAdminToken(ADMIN_TOKEN); // Optional

    @Test
    public void someTestMethod() {
        final InfluxDBClient influxDB = influxDbContainer.getInfluxDBClient();
        // rest of the test
    }
}
```

**NOTE**: You can find the latest documentation about the influxdb java
client [here](https://github.com/influxdata/influxdb-client-java).

## InfluxDB V1.x Usage example (Deprecated)

Running influxDbContainer as a stand-in for InfluxDB in a test:

```java
public class SomeTest {

    @Rule
    public InfluxDBContainer influxDbContainer = new InfluxDBContainer();

    @Test
    public void someTestMethod() {
        final InfluxDB influxDB = this.influxDbContainer.getNewInfluxDB();
        // Rest of the test
    }
}
```

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
