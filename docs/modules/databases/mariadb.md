# MariaDB Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Usage example

Running MariaDB as a stand-in for in a test:

```java
public class SomeTest {

    @Rule
    public MariaDBContainer mariaDB = new MariaDBContainer();
    
    @Test
    public void someTestMethod() {
        String url = mariaDB.getJdbcUrl();

        ... create a connection and run test as normal
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:mariadb:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mariadb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.

