# Trino Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

See [Database containers](./index.md) for documentation and usage that is common to all database container types.

## Usage example

Running Trino as a stand-in for in a test:

```java
public class SomeTest {

    @Rule
    public TrinoContainer trino = new TrinoContainer();
    
    @Test
    public void someTestMethod() {
        String url = trino.getJdbcUrl();

        ... create a connection and run test as normal
```

Trino comes with several catalogs preconfigured. Most useful ones for testing are

* `tpch` catalog using the [Trino TPCH Connector](https://trino.io/docs/current/connector/tpch.html).
  This is a read-only catalog that defines standard TPCH schema, so is available for querying without a need
  to create any tables.
* `memory` catalog using the [Trino Memory Connector](https://trino.io/docs/current/connector/memory.html).
  This catalog can be used for creating schemas and tables and does not require any storage, as everything
  is stored fully in-memory.

Example test using the `tpch` and `memory` catalogs:

```java
public class SomeTest {
    @Rule
    public TrinoContainer trino = new TrinoContainer();

    @Test
    public void queryMemoryAndTpchConnectors() throws SQLException {
        try (Connection connection = trino.createConnection();
             Statement statement = connection.createStatement()) {
            // Prepare data
            statement.execute("CREATE TABLE memory.default.table_with_array AS SELECT 1 id, ARRAY[1, 42, 2, 42, 4, 42] my_array");

            // Query Trino using newly created table and a builtin connector
            try (ResultSet resultSet = statement.executeQuery("" +
                "SELECT nationkey, element " +
                "FROM tpch.tiny.nation " +
                "JOIN memory.default.table_with_array twa ON nationkey = twa.id " +
                "LEFT JOIN UNNEST(my_array) a(element) ON true " +
                "ORDER BY element OFFSET 1 FETCH NEXT 3 ROWS WITH TIES ")) {
                List<Integer> actualElements = new ArrayList<>();
                while (resultSet.next()) {
                    actualElements.add(resultSet.getInt("element"));
                }
                Assert.assertEquals(Arrays.asList(2, 4, 42, 42, 42), actualElements);
            }
        }
    }
}
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:trino:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>trino</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

!!! hint
    Adding this Testcontainers library JAR will not automatically add the Trino JDBC driver JAR to your project.
    You should ensure that your project has the Trino JDBC driver as a dependency, if you plan on using it.
    Refer to [Trino project download page](https://trino.io/download.html) for instructions.


