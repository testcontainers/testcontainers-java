# Cassandra Module

## Usage example

This example connects to the Cassandra Cluster, creates a keyspaces and asserts that is has been created.

```java tab="Cassandra Driver 4.x"
public class SomeTest {

    @Rule
    public CassandraContainer cassandra = new CassandraContainer();


    @Test
    public void test(){
        try(CqlSession session = CqlSession.builder()
                .addContactPoint(cassandra.getContactPoint())
                .withLocalDatacenter(cassandra.getLocalDatacenter())
                .build()) {

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

```java tab="Cassandra Driver 3.x"
public class SomeTest {

    @Rule
    public CassandraContainer cassandra = new CassandraContainer();


    @Test
    public void test(){
        Cluster cluster = Cluster.builder()
                .addContactPoint(cassandra.getHost())
                .withPort(cassandra.getMappedPort(CassandraContainer.CQL_PORT))
                .build();

        try(Session session = cluster.connect()) {

            session.execute("CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
                    "{'class':'SimpleStrategy','replication_factor':'1'};");

            List<KeyspaceMetadata> keyspaces = session.getCluster().getMetadata().getKeyspaces();
            List<KeyspaceMetadata> filteredKeyspaces = keyspaces
                    .stream()
                    .filter(km -> km.getName().equals("test"))
                    .collect(Collectors.toList());

            assertEquals(1, filteredKeyspaces.size());
        }
    }

}
```

!!! warning
    All methods returning instances of the Cassandra Driver's Cluster object in CassandraContainer have been deprecated. Providing these methods unnecessarily couples the Container to the Driver and creates potential breaking changes if the driver is updated.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:cassandra:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>cassandra</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
