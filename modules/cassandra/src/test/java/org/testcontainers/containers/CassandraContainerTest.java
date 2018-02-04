package org.testcontainers.containers;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Eugeny Karpov
 */
@Slf4j
public class CassandraContainerTest {

    private static final String TEST_CLUSTER_NAME_IN_CONF = "Test Cluster Integration Test";

    @Test
    public void testSimple() throws Exception {
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer()
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertNotNull("Result set has no release_version", resultSet.one().getString(0));
        }
    }

    @Test
    public void testSpecificVersion() throws Exception {
        String cassandraVersion = "3.0.15";
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer("cassandra:" + cassandraVersion)
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertEquals("Cassandra has wrong version", cassandraVersion, resultSet.one().getString(0));
        }
    }

    @Test
    public void testConfigurationOverride() throws Exception {
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer()
                .withConfigurationOverride("cassandra-test-configuration-example")
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT cluster_name FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
            assertEquals("Cassandra configuration is not overridden", TEST_CLUSTER_NAME_IN_CONF, resultSet.one().getString(0));
        }
    }

    @Test(expected = ContainerLaunchException.class)
    public void testEmptyConfigurationOverride() throws Exception {
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer()
                .withConfigurationOverride("cassandra-empty-configuration")
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
        }
    }

    @Test
    public void testInitScript() throws Exception {
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer()
                .withInitScript("initial.cql")
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
            testInitScript(cassandraContainer);
        }
    }

    @Test
    public void testInitScriptWithLegacyCassandra() throws Exception {
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer("cassandra:2.2.11")
                .withInitScript("initial.cql")
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
            testInitScript(cassandraContainer);
        }
    }

    @Test
    public void testCassandraQueryWaitStrategy() throws Exception {
        try (CassandraContainer cassandraContainer = (CassandraContainer) new CassandraContainer()
                .waitingFor(new CassandraQueryWaitStrategy())
                .withLogConsumer(new Slf4jLogConsumer(log))) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, "SELECT release_version FROM system.local");
            assertTrue("Query was not applied", resultSet.wasApplied());
        }
    }

    private void testInitScript(CassandraContainer cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category");
        assertTrue("Query was not applied", resultSet.wasApplied());
        Row row = resultSet.one();
        assertEquals("Inserted row is not in expected state", 1, row.getLong(0));
        assertEquals("Inserted row is not in expected state", "test_category", row.getString(1));
    }

    private ResultSet performQuery(CassandraContainer cassandraContainer, String cql) {
        try (Cluster cluster = Cluster.builder()
                .addContactPoint(cassandraContainer.getContainerIpAddress())
                .withPort(cassandraContainer.getMappedPort(CassandraContainer.CQL_PORT))
                .build()) {
            Session session = cluster.newSession();

            return session.execute(cql);
        }
    }
}
