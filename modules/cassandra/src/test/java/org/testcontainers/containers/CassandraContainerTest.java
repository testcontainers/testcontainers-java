package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.schema.KeyspaceMetadata;
import com.datastax.oss.driver.api.core.session.Session;
import java.net.InetSocketAddress;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugeny Karpov
 */
@Slf4j
public class CassandraContainerTest {

    private static final DockerImageName CASSANDRA_IMAGE = DockerImageName.parse("cassandra:3.11.2");

    private static final String TEST_CLUSTER_NAME_IN_CONF = "Test Cluster Integration Test";

    @Test
    public void testSimple() {
        try (CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE)) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertNotNull("Result set has no release_version", resultSet.one().getString(0));
        }
    }

    @Test
    public void testSpecificVersion() {
        String cassandraVersion = "3.0.15";
        try (CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE.withTag(cassandraVersion))) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertEquals("Cassandra has wrong version", cassandraVersion, resultSet.one().getString(0));
        }
    }

    @Test
    public void testConfigurationOverride() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE)
                .withConfigurationOverride("cassandra-test-configuration-example")
        ) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT cluster_name FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertEquals("Cassandra configuration is not overridden", TEST_CLUSTER_NAME_IN_CONF, resultSet.one().getString(0));
        }
    }

    @Test(expected = ContainerLaunchException.class)
    public void testEmptyConfigurationOverride() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE)
                .withConfigurationOverride("cassandra-empty-configuration")
        ) {
            cassandraContainer.start();
        }
    }

    @Test
    public void testInitScript() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE)
                .withInitScript("initial.cql")
        ) {
            cassandraContainer.start();
            testInitScript(cassandraContainer);
        }
    }

    @Test
    public void testInitScriptWithLegacyCassandra() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(DockerImageName.parse("cassandra:2.2.11"))
                .withInitScript("initial.cql")
        ) {
            cassandraContainer.start();
            testInitScript(cassandraContainer);
        }
    }

    @Test
    public void testKeyspace(){
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

    @SuppressWarnings("deprecation") // Using deprecated constructor for verification of backwards compatibility
    @Test
    public void testCassandraQueryWaitStrategy() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>()
                .waitingFor(new CassandraQueryWaitStrategy())
        ) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
        }
    }

    private void testInitScript(CassandraContainer<?> cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category");
        assertTrue("Query was not applied", resultSet.wasApplied());
        Row row = resultSet.one();
        assertEquals("Inserted row is not in expected state", 1, row.getLong(0));
        assertEquals("Inserted row is not in expected state", "test_category", row.getString(1));
    }

    private ResultSet performQuery(CassandraContainer<?> cassandraContainer, String cql) {
        CqlSession session = CqlSession.builder()
            .addContactPoint(new InetSocketAddress(cassandraContainer.getHost(), cassandraContainer.getMappedPort(CassandraContainer.CQL_PORT)))
            .withLocalDatacenter("datacenter1")
            .build();
        return session.execute(cql);
    }
}
