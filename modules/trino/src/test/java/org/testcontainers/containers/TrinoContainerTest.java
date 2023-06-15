package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.TrinoTestImages;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

public class TrinoContainerTest {

    @Test
    public void testSimple() throws Exception {
        try (TrinoContainer trino = new TrinoContainer(TrinoTestImages.TRINO_TEST_IMAGE)) {
            trino.start();
            try (
                Connection connection = trino.createConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT DISTINCT node_version FROM system.runtime.nodes")
            ) {
                assertThat(resultSet.next()).as("results").isTrue();
                assertThat(resultSet.getString("node_version"))
                    .as("Trino version")
                    .isEqualTo(TrinoContainer.DEFAULT_TAG);
                assertContainerHasCorrectExposedAndLivenessCheckPorts(trino);
            }
        }
    }

    @Test
    public void testSpecificVersion() throws Exception {
        try (TrinoContainer trino = new TrinoContainer(TrinoTestImages.TRINO_PREVIOUS_VERSION_TEST_IMAGE)) {
            trino.start();
            try (
                Connection connection = trino.createConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT DISTINCT node_version FROM system.runtime.nodes")
            ) {
                assertThat(resultSet.next()).as("results").isTrue();
                assertThat(resultSet.getString("node_version"))
                    .as("Trino version")
                    .isEqualTo(TrinoTestImages.TRINO_PREVIOUS_VERSION_TEST_IMAGE.getVersionPart());
            }
        }
    }

    @Test
    public void testInitScript() throws Exception {
        try (TrinoContainer trino = new TrinoContainer(TrinoTestImages.TRINO_TEST_IMAGE)) {
            trino.withInitScript("initial.sql");
            trino.start();
            try (
                Connection connection = trino.createConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT a FROM memory.default.test_table")
            ) {
                assertThat(resultSet.next()).as("results").isTrue();
                assertThat(resultSet.getObject("a")).as("Value").isEqualTo(12345678909324L);
                assertThat(resultSet.next()).as("results").isFalse();
            }
        }
    }

    private void assertContainerHasCorrectExposedAndLivenessCheckPorts(TrinoContainer trino) {
        assertThat(trino.getExposedPorts()).containsExactly(8080);
        assertThat(trino.getLivenessCheckPortNumbers()).containsExactly(trino.getMappedPort(8080));
    }
}
