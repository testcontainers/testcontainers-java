package org.testcontainers.containers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;

import static org.junit.Assert.*;

/**
 * @author Eugeny Karpov
 */
@Slf4j
public class CassandraContainerTest {

    private static final String TEST_CLUSTER_NAME_IN_CONF = "Test Cluster Integration Test";

    @Test
    public void testSimple() {
        try (CassandraContainer cassandraContainer = new CassandraContainer<>()) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertNotNull("Result set has no release_version", resultSet.one().getString(0));
        }
    }

    @Test
    public void testSpecificVersion() {
        String cassandraVersion = "3.0.15";
        try (CassandraContainer cassandraContainer = new CassandraContainer<>("cassandra:" + cassandraVersion)) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertEquals("Cassandra has wrong version", cassandraVersion, resultSet.one().getString(0));
        }
    }

    @Test
    public void testConfigurationOverride() {
        try (
            CassandraContainer cassandraContainer = new CassandraContainer<>()
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
            CassandraContainer cassandraContainer = new CassandraContainer<>()
                .withConfigurationOverride("cassandra-empty-configuration")
        ) {
            cassandraContainer.start();
        }
    }

    @Test
    public void testInitScript() {
        try (
            CassandraContainer cassandraContainer = new CassandraContainer<>().withInitScript("initial-1.cql")
        ) {
            cassandraContainer.start();
            testInitScript1(cassandraContainer);
        }
    }

    @Test
    public void testMultipleInitScripts() {
        try (
            CassandraContainer cassandraContainer = new CassandraContainer<>()
                .withInitScript("initial-1.cql", "initial-2.cql")
        ) {
            cassandraContainer.start();
            testInitScript1(cassandraContainer);
            testInitScript2(cassandraContainer);
        }
    }

    @Test
    public void testInitScriptWithLegacyCassandra() {
        try (
            CassandraContainer cassandraContainer = new CassandraContainer<>("cassandra:2.2.11")
                .withInitScript("initial-1.cql")
        ) {
            cassandraContainer.start();
            testInitScript1(cassandraContainer);
        }
    }

    @Test
    public void testCassandraQueryWaitStrategy() {
        try (
            CassandraContainer cassandraContainer = new CassandraContainer<>()
                .waitingFor(new CassandraQueryWaitStrategy())
        ) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
        }
    }

    @Test
    public void testCassandraGetCluster() {
        try (CassandraContainer cassandraContainer = new CassandraContainer<>()) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer.getCluster(), "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertNotNull("Result set has no release_version", resultSet.one().getString(0));
        }
    }

    private void testInitScript1(CassandraContainer cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category WHERE id = 1");
        assertTrue("Query was not applied", resultSet.wasApplied());
        Row row = resultSet.one();
        assertEquals("Inserted row is not in expected state", 1, row.getLong(0));
        assertEquals("Inserted row is not in expected state", "test_category", row.getString(1));
    }

    private void testInitScript2(CassandraContainer cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category WHERE id = 2");
        assertTrue("Query was not applied", resultSet.wasApplied());
        Row row = resultSet.one();
        assertEquals("Inserted row is not in expected state", 2, row.getLong(0));
        assertEquals("Inserted row is not in expected state", "another_test_category", row.getString(1));
    }

    private ResultSet performQuery(CassandraContainer cassandraContainer, String cql) {
        Cluster explicitCluster = Cluster.builder()
            .addContactPoint(cassandraContainer.getContainerIpAddress())
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
