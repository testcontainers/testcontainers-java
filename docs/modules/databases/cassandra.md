# Cassandra Module

## Usage example

This example connects to the Cassandra Cluster, creates a keyspaces and asserts that is has been created.

```java tab="JUnit 4 example"
public class SomeTest {

    @Rule
    public CassandraContainer cassandra = new CassandraContainer();


    @Test
    public void test(){
        Cluster cluster = cassandra.getCluster();

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
