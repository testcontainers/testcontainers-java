package org.testcontainers.containers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.Comparator;
import java.util.List;

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
    public void testInitScripts() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE)
                .withInitScripts("initial.cql", "initial_2.cql")
        ) {
            cassandraContainer.start();
            testInitScripts(cassandraContainer);
        }
    }

    @Test
    public void testInitScriptsWithLegacyCassandra() {
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(DockerImageName.parse("cassandra:2.2.11"))
                .withInitScripts("initial.cql", "initial_2.cql")
        ) {
            cassandraContainer.start();
            testInitScripts(cassandraContainer);
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

    @SuppressWarnings("deprecation") // Using deprecated constructor for verification of backwards compatibility
    @Test
    public void testCassandraGetCluster() {
        try (CassandraContainer<?> cassandraContainer = new CassandraContainer<>()) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer.getCluster(), "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertNotNull("Result set has no release_version", resultSet.one().getString(0));
        }
    }

    private void testInitScript(CassandraContainer<?> cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category");
        assertTrue("Query was not applied", resultSet.wasApplied());
        Row row = resultSet.one();
        assertEquals("Inserted row is not in expected state", 1, row.getLong(0));
        assertEquals("Inserted row is not in expected state", "test_category", row.getString(1));
    }

    private void testInitScripts(CassandraContainer<?> cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category");
        assertTrue("Query was not applied", resultSet.wasApplied());
        List<Row> rows = resultSet.all();
        assertEquals("The number of returned records is unexpected", 2, rows.size());
        rows.sort(Comparator.comparingLong(row -> row.getLong(0)));
        assertEquals("Inserted row is not in expected state", 1, rows.get(0).getLong(0));
        assertEquals("Inserted row is not in expected state", "test_category", rows.get(0).getString(1));
        assertEquals("Inserted row is not in expected state", 2, rows.get(1).getLong(0));
        assertEquals("Inserted row is not in expected state", "test_category_2", rows.get(1).getString(1));
    }

    private ResultSet performQuery(CassandraContainer<?> cassandraContainer, String cql) {
        Cluster explicitCluster = Cluster.builder()
            .addContactPoint(cassandraContainer.getHost())
            .withPort(cassandraContainer.getMappedPort(CassandraContainer.CQL_PORT))
            .build();
        return performQuery(explicitCluster, cql);
    }

    private ResultSet performQuery(Cluster cluster, String cql) {
        try (Cluster closeableCluster = cluster) {
            Session session = closeableCluster.newSession();
            return session.execute(cql);
        }
    }
}
