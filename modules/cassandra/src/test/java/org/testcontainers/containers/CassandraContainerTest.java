package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.wait.CassandraQueryWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CassandraContainerTest {

    private static final DockerImageName CASSANDRA_IMAGE = DockerImageName.parse("cassandra:3.11.2");

    private static final String TEST_CLUSTER_NAME_IN_CONF = "Test Cluster Integration Test";

    private static final String BASIC_QUERY = "SELECT release_version FROM system.local";

    @Test
    public void testSimple() {
        try (CassandraContainer<?> cassandraContainer = new CassandraContainer<>(CASSANDRA_IMAGE)) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0)).as("Result set has release_version").isNotNull();
        }
    }

    @Test
    public void testSpecificVersion() {
        String cassandraVersion = "3.0.15";
        try (
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(
                CASSANDRA_IMAGE.withTag(cassandraVersion)
            )
        ) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer, BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0)).as("Cassandra has right version").isEqualTo(cassandraVersion);
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
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0))
                .as("Cassandra configuration is overridden")
                .isEqualTo(TEST_CLUSTER_NAME_IN_CONF);
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
            CassandraContainer<?> cassandraContainer = new CassandraContainer<>(
                DockerImageName.parse("cassandra:2.2.11")
            )
                .withInitScript("initial.cql")
        ) {
            cassandraContainer.start();
            testInitScript(cassandraContainer);
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
            ResultSet resultSet = performQuery(cassandraContainer, BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
        }
    }

    @SuppressWarnings("deprecation") // Using deprecated constructor for verification of backwards compatibility
    @Test
    public void testCassandraGetCluster() {
        try (CassandraContainer<?> cassandraContainer = new CassandraContainer<>()) {
            cassandraContainer.start();
            ResultSet resultSet = performQuery(cassandraContainer.getCqlSession(), BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0)).as("Result set has release_version").isNotNull();
        }
    }

    private void testInitScript(CassandraContainer<?> cassandraContainer) {
        ResultSet resultSet = performQuery(cassandraContainer, "SELECT * FROM keySpaceTest.catalog_category");
        assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
        Row row = resultSet.one();
        assertThat(row.getLong(0)).as("Inserted row is in expected state").isEqualTo(1);
        assertThat(row.getString(1)).as("Inserted row is in expected state").isEqualTo("test_category");
    }

    private ResultSet performQuery(CassandraContainer<?> cassandraContainer, String cql) {
        final CqlSession cqlSession = CqlSession
            .builder()
            .addContactPoint(
                new InetSocketAddress(
                    cassandraContainer.getHost(),
                    cassandraContainer.getMappedPort(CassandraContainer.CQL_PORT)
                )
            )
            .withLocalDatacenter(cassandraContainer.getLocalDatacenter())
            .build();
        return performQuery(cqlSession, cql);
    }

    private ResultSet performQuery(CqlSession session, String cql) {
        final ResultSet rs = session.execute(cql);
        session.close();
        return rs;
    }
}
