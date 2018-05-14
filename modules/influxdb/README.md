# Testcontainers InfluxDB testing module

Testcontainers module for the InfluxData [InfluxDB](https://github.com/influxdata/influxdb).

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

## Dependency information

Replace `VERSION` with the [latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22).

[![](https://api.bintray.com/packages/testcontainers/releases/testcontainers/images/download.svg)](https://bintray.com/testcontainers/releases/testcontainers/_latestVersion)


### Maven
```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>influxdb</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'influxdb', version: 'VERSION'
```
