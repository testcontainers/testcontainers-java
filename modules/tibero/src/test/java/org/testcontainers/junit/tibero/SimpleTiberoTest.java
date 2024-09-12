package org.testcontainers.junit.tibero;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.tibero.TiberoContainer;
import org.testcontainers.utility.DockerImageName;

public class SimpleTiberoTest extends AbstractContainerDatabaseTest {

    public static final DockerImageName TIBERO_DOCKER_IMAGE_NAME = DockerImageName
        .parse("ghcr.io/tibero-support/tibero7");

    private void runTest(TiberoContainer tiberoContainer, String databasename, String username, String password)
        throws SQLException {
        SoftAssertions assertions = new SoftAssertions();

        assertions.assertThat(tiberoContainer.getDatabaseName()).isEqualTo(databasename);
        assertions.assertThat(tiberoContainer.getUsername()).isEqualTo(username);
        assertions.assertThat(tiberoContainer.getPassword()).isEqualTo(password);

        // Test we can get a connection
        tiberoContainer.start();

        ResultSet resultSet = performQuery(tiberoContainer, "SELECT 1 FROM DUAL");
        int resultSetInt = resultSet.getInt(1);

        assertions.assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);

        assertions.assertAll();
    }

    @Test
    public void testDefaultSettings() throws SQLException {
        try (
            TiberoContainer tiberoContainer = new TiberoContainer(TIBERO_DOCKER_IMAGE_NAME)
                .withDatabaseName("tibero")
                .withUsername("tibero")
                .withPassword("tibero")
        ) {
            runTest(tiberoContainer, "tibero", "tibero", "tibero");
        }
    }


    @Test
    public void testErrorPaths() {
        try (TiberoContainer tibero = new TiberoContainer(TIBERO_DOCKER_IMAGE_NAME)) {
            SoftAssertions assertions = new SoftAssertions();

            assertions.assertThatThrownBy(() -> tibero.withDatabaseName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Database name cannot be null or empty");

            assertions.assertThatThrownBy(() -> tibero.withDatabaseName("sys"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Database name cannot be set to sys");

            assertions.assertThatThrownBy(() -> tibero.withUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be null or empty");

            assertions.assertThatThrownBy(() -> tibero.withUsername("sys"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username cannot be sys");

            assertions.assertThatThrownBy(() -> tibero.withPassword(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Password cannot be null or empty");

            assertions.assertAll();
        }
    }
}
