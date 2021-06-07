# InfluxDB Module

Testcontainers module for InfluxData [InfluxDB](https://github.com/influxdata/influxdb).

## Important Note
They are breaking changes in InfluxDB v2.x. For more information refer to the main [documentation](https://docs.influxdata.com/influxdb/v2.0/upgrade/v1-to-v2/). 
Influx Data migrated to quay.io as their new [container registry](https://quay.io/repository/influxdb/influxdb).

## InfluxDB V2.x Usage example
Running influxDbContainer as a stand-in for InfluxDB in a test:

```java
public class SomeTest {

    @ClassRule
    public static final InfluxDBV2Container influxDbContainer = InfluxDBV2Container.createWithDefaultTag();

    @Test
    public void someTestMethod() {
        final InfluxDBClient influxDB = influxDbContainer.getNewInfluxDB();
        // Rest of the test
    }
}
```
This will create a InfluxDB container with tag v2.0.0. The influxDB will be setup with the following data:<br/>

| Property      | Default Value | 
| ------------- |:-------------:|
| User          | test-user     | 
| Password      | test-password | 
| Bucket        | test-bucket   |  
| Organization  | test-org      |
| Retention     | 0 (infinite)  |
| Retention Unit| ns            |
For more details about the InfluxDB setup go to the official [docs](https://docs.influxdata.com/influxdb/v2.0/reference/cli/influx/setup/).

It is possible to override the default values and start the container with a newer tag:
```java
public class SomeTest {
    @ClassRule
    public static final InfluxDBContainerV2<?> influxDBContainer = InfluxDBContainerV2
        .createWithSpecificTag(DockerImageName.parse("quay.io/influxdb/influxdb:v2.0.3"))
        .withBucket(BUCKET)
        .withUsername(USER)
        .withPassword(PASSWORD)
        .withOrganization(ORG);

    @Test
    public void someTestMethod() {
        final InfluxDBClient influxDB = influxDbContainer.getNewInfluxDB();
        // rest of the test
    }
}
```
**NOTE**: The `createWithSpecificTag` static factory needs a valid registry and tag name. You can find the latest tags [here](https://quay.io/repository/influxdb/influxdb?tab=tags). <br/>
**NOTE**: You can find the latest documentation about the influxdb java client [here](https://github.com/influxdata/influxdb-client-java).

**Hint:**
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

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
