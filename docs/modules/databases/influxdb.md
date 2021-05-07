# InfluxDB Module

Testcontainers module for InfluxData [InfluxDB](https://github.com/influxdata/influxdb).

## Usage example

Running influxDbContainer as a stand-in for InfluxDB in a test:

```java
public class SomeTest {

    @Rule
    public InfluxDBContainer influxDbContainer = new InfluxDBContainer();
    
    @Test
    public void someTestMethod() {
         InfluxDB influxDB = influxDbContainer.getNewInfluxDB();
         ...
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

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.
