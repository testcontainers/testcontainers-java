# Testcontainers MariaDB Module

Testcontainers module for the MariaDB database.

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

## Dependency information

Replace `VERSION` with the [latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22).

### Maven

```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mariadb</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'mariadb', version: 'VERSION'
```
