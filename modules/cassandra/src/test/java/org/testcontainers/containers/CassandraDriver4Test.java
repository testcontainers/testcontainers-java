package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.junit4.Container;
import org.testcontainers.junit4.TestContainersRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class CassandraDriver4Test {

    @Container
    public CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:3.11.2");

    @Test
    public void testCassandraGetContactPoint() {
        try (
            CqlSession session = CqlSession
                .builder()
                .addContactPoint(this.cassandra.getContactPoint())
                .withLocalDatacenter(this.cassandra.getLocalDatacenter())
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
