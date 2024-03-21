package org.testcontainers.containers;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.wait.ScyllaDBQueryWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.ScyllaDBContainer.CQL_PORT;

@Slf4j
public class ScyllaDBContainerTest {

    private static final DockerImageName SCYLLADB_IMAGE = DockerImageName.parse("scylladb/scylla:5.2.9");

    private static final String TEST_CLUSTER_NAME_IN_CONF = "Test Cluster Integration Test";

    private static final String BASIC_QUERY = "SELECT release_version FROM system.local";

    @Test
    public void testSimple() {
        try (ScyllaDBContainer<?> scylladbContainer = new ScyllaDBContainer<>(SCYLLADB_IMAGE)) {
            scylladbContainer.start();
            ResultSet resultSet = performQuery(scylladbContainer, BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0)).as("Result set has release_version").isNotNull();
        }
    }

    @Test
    public void testSpecificVersion() {
        String scyllaDBReportedVersion = "3.0.8";
        try (
            ScyllaDBContainer<?> ScyllaDBContainer = new ScyllaDBContainer<>(SCYLLADB_IMAGE)
        ) {
            ScyllaDBContainer.start();
            ResultSet resultSet = performQuery(ScyllaDBContainer, BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0)).as("ScyllaDB has right version").isEqualTo(scyllaDBReportedVersion);
        }
    }

    @Test
    public void testConfigurationOverride() {
        try (
            ScyllaDBContainer<?> ScyllaDBContainer = new ScyllaDBContainer<>(SCYLLADB_IMAGE)
                .withConfigurationOverride("scylla-test-configuration-example")
        ) {
            ScyllaDBContainer.start();
            ResultSet resultSet = performQuery(ScyllaDBContainer, "SELECT cluster_name FROM system.local");
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
            assertThat(resultSet.one().getString(0))
                .as("ScyllaDB configuration is overridden")
                .isEqualTo(TEST_CLUSTER_NAME_IN_CONF);
        }
    }

    @Test(expected = ContainerLaunchException.class)
    public void testEmptyConfigurationOverride() {
        try (
            ScyllaDBContainer<?> ScyllaDBContainer = new ScyllaDBContainer<>(SCYLLADB_IMAGE)
                .withConfigurationOverride("scylladb-empty-configuration")
        ) {
            ScyllaDBContainer.start();
        }
    }

    @Test
    public void testInitScript() {
        try (
            ScyllaDBContainer<?> ScyllaDBContainer = new ScyllaDBContainer<>(SCYLLADB_IMAGE)
                .withInitScript("initial.cql")
        ) {
            ScyllaDBContainer.start();
            testInitScript(ScyllaDBContainer);
        }
    }

    @Test
    public void testScyllaDBQueryWaitStrategy() {
        try (
            ScyllaDBContainer<?> scyllaDBContainer = new ScyllaDBContainer<>(SCYLLADB_IMAGE)
                .waitingFor(new ScyllaDBQueryWaitStrategy())
        ) {
            scyllaDBContainer.start();
            ResultSet resultSet = performQuery(scyllaDBContainer, BASIC_QUERY);
            assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
        }
    }

    private void testInitScript(ScyllaDBContainer<?> ScyllaDBContainer) {
        ResultSet resultSet = performQuery(ScyllaDBContainer, "SELECT * FROM keySpaceTest.catalog_category");
        assertThat(resultSet.wasApplied()).as("Query was applied").isTrue();
        Row row = resultSet.one();
        assertThat(row.getLong(0)).as("Inserted row is in expected state").isEqualTo(1);
        assertThat(row.getString(1)).as("Inserted row is in expected state").isEqualTo("test_category");
    }

    private ResultSet performQuery(ScyllaDBContainer<?> ScyllaDBContainer, String cql) {
        CqlSession session = CqlSession.
            builder().
            addContactPoint(
                new InetSocketAddress(
                    ScyllaDBContainer.getHost(),
                    ScyllaDBContainer.getMappedPort(CQL_PORT)
                )).
            withLocalDatacenter("datacenter1").
            build();

        return session.execute(cql);
    }
}
