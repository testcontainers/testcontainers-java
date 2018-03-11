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

### Maven

```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mariadb</artifactId>
    <version>1.4.3</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'mariadb', version: '1.4.3'
```
