package org.testcontainers.containers;
// cassandra4 {
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class CassandraDriver4Test {

    @Rule
    public CassandraContainer cassandra = new CassandraContainer("cassandra:3.11.2");

    @Test
    public void test() {
        try (CqlSession session = CqlSession.builder()
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
//}
