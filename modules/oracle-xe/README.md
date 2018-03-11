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

### Maven

```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>oracle-xe</artifactId>
    <version>1.4.3</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'oracle-xe', version: '1.4.3'
```
