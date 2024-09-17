package org.testcontainers.containers.junit.vertica;

import org.junit.Test;
import org.testcontainers.containers.VerticaCEContainer;
import org.testcontainers.containers.VerticaCETestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleVerticaCETest extends AbstractContainerDatabaseTest {

    // static {
    //    LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    // }

    @Test
    public void testSimple() throws SQLException {
        try (VerticaCEContainer<?> vertica = new VerticaCEContainer<>(VerticaCETestImages.VERTICA_CE_TEST_IMAGE)) {
            vertica.start();

            ResultSet resultSet = performQuery(vertica, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(vertica);
        }
    }

    /**
     * Vertica exposes both a database port (5433) and one or more data warehouse ports (5444, ...). We
     * only care about the database port.
     *
     * @param vertica
     */
    private void assertHasCorrectExposedAndLivenessCheckPorts(VerticaCEContainer<?> vertica) {
        assertThat(vertica.getExposedPorts()).contains(VerticaCEContainer.VERTICA_DATABASE_PORT);
        assertThat(vertica.getLivenessCheckPortNumbers())
            .contains(vertica.getMappedPort(VerticaCEContainer.VERTICA_DATABASE_PORT));
    }
}
