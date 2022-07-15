package org.testcontainers.containers;
// cassandra3 {
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.KeyspaceMetadata;
import com.datastax.driver.core.Session;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class CassandraDriver3Test {

    @Rule
    public CassandraContainer cassandra = new CassandraContainer("cassandra:3.11.2");

    @Test
    public void test() {
        try (Cluster cluster = Cluster.builder()
            .addContactPoint(cassandra.getHost())
            .withPort(cassandra.getMappedPort(CassandraContainer.CQL_PORT))
            .build();
             Session session = cluster.connect()) {

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
// }
