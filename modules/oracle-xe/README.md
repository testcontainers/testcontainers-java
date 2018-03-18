# Testcontainers Oracle XE Module

Testcontainers module for the Oracle XE database.

## Usage example

Running Oracle XE as a stand-in for in a test:

```java
public class SomeTest {

    @Rule
    public OracleContainer oracle = new OracleContainer();
    
    @Test
    public void someTestMethod() {
        String url = oracle.getJdbcUrl();

        ... create a connection and run test as normal
```

## Dependency information

Replace `VERSION` with the [latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22).

### Maven

```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>oracle-xe</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'oracle-xe', version: 'VERSION'
```
