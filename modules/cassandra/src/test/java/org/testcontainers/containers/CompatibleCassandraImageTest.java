package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@MethodSource("params")
public class CompatibleCassandraImageTest {

    public static String[] params() {
        return new String[] { "cassandra:3.11.2", "cassandra:4.1.1" };
    }

    @Parameter(0)
    public String imageName;

    @Test
    public void testCassandraGetContactPoint() {
        try (CassandraContainer<?> cassandra = new CassandraContainer<>(this.imageName)) {
            cassandra.start();
            assertCassandraFunctionality(cassandra);
        }
    }

    private void assertCassandraFunctionality(CassandraContainer<?> cassandra) {
        try (
            CqlSession session = CqlSession
                .builder()
                .addContactPoint(cassandra.getContactPoint())
                .withLocalDatacenter(cassandra.getLocalDatacenter())
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
