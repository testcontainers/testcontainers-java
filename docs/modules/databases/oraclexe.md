# Oracle-XE Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Usage example

Running Oracle XE as a stand-in for in a test:

```java
public class SomeTest {

    @Rule
    public OracleContainer oracle = new OracleContainer("name_of_your_oracle_xe_image");
    
    @Test
    public void someTestMethod() {
        String url = oracle.getJdbcUrl();

        ... create a connection and run test as normal
```

## Specifying a docker image name via config

If you do not pass an image name to the `OracleContainer` constructor, a suitable image name should be placed in
configuration instead.
To do this, please place a file on the classpath named `testcontainers.properties`,
containing `oracle.container.image=IMAGE`, where IMAGE is a suitable image name and tag.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:oracle-xe:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>oracle-xe</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.


