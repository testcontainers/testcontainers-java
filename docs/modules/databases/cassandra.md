# Cassandra Module

## Usage example

This example connects to the Cassandra Cluster, creates a keyspaces and asserts that is has been created.

```java tab="JUnit 4 example"
public class SomeTest {

    @Test
    public void test(){
         try (CassandraContainer<?> cassandra = new CassandraContainer<>(CASSANDRA_IMAGE)) {
            cassandra.start();

        CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(cassandra.CQL_PORT)))
                .withLocalDatacenter("datacenter1")
                .build();

            session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
                    "{'class':'SimpleStrategy','replication_factor':'1'};");

            KeyspaceMetadata keyspace = session
                    .getMetadata()
                    .getKeyspaces()
                    .get(CqlIdentifier.fromCql("test"));

            assertNotNull("Failed to create test keyspace", keyspace);
        }
    }
}
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:cassandra:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>cassandra</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
