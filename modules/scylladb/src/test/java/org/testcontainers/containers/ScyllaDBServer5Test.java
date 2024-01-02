package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScyllaDBServer5Test {

    @Rule
    public ScyllaDBContainer<?> scylladb = new ScyllaDBContainer<>("scylladb/scylla:5.2.9");

    @Test
    public void testScyllaDBGetContactPoint() {
        try (
            CqlSession session = CqlSession
                .builder()
                .addContactPoint(this.scylladb.getContactPoint())
                .withLocalDatacenter(this.scylladb.getLocalDatacenter())
                .build()
        ) {
            session.execute(
                "CREATE KEYSPACE IF NOT EXISTS test WITH replication = \n" +
                "{'class':'SimpleStrategy','replication_factor':'1'};"
            );

            KeyspaceMetadata keyspace = session.getMetadata().getKeyspaces().get(CqlIdentifier.fromCql("test"));

            assertThat(keyspace).as("test keyspace created").isNotNull();
        }
    }
}
