# YugabyteDB Module

See [Database containers](./index.md) for documentation and usage that is common to all database container types.

YugabyteDB supports two APIs. 
- Yugabyte Structured Query Language [YSQL](https://docs.yugabyte.com/latest/api/ysql/) is a fully-relational API that is built by the PostgreSQL code
- Yugabyte Cloud Query Language [YCQL](https://docs.yugabyte.com/latest/api/ycql/) is a semi-relational SQL API that has its roots in the Cassandra Query Language

## Usage example

### YSQL API 

```java
public class YugabyteDBTest {

    @Rule
    public YugabyteYSQLContainer container = new YugabyteYSQLContainer("yugabytedb/yugabyte:2.7.2.0-b216");
    
    @Test
    public void method() {
        String url = container.getJdbcUrl();

        ... create a connection and run the tests as usual. It also depends on the frameworks being used.
```

#### JDBC URL

`jdbc:tc:yugabyte:2.7.2.0-b216:///yugabyte`

### YCQL API

```java
public class YugabyteDBTest {

    @Rule
    public YugabyteYCQLContainer container = new YugabyteYCQLContainer("yugabytedb/yugabyte:2.7.2.0-b216");
    
    @Test
    public void method() {
        Session session = container.getSession();

        ... create a connection and run the tests as usual. It also depends on the frameworks being used.
```

## Adding this module to your project dependencies
[[TODO]]
