package org.testcontainers.junit.mysql;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@ParameterizedClass
@MethodSource("params")
public class MultiVersionMySQLTest extends AbstractContainerDatabaseTest {

    public static DockerImageName[] params() {
        return new DockerImageName[] {
            MySQLTestImages.MYSQL_57_IMAGE,
            MySQLTestImages.MYSQL_80_IMAGE,
            MySQLTestImages.MYSQL_INNOVATION_IMAGE,
        };
    }

    @Parameter(0)
    public DockerImageName dockerImageName;

    @Test
    public void versionCheckTest() throws SQLException {
        try (MySQLContainer<?> mysql = new MySQLContainer<>(dockerImageName)) {
            mysql.start();
            final ResultSet resultSet = performQuery(mysql, "SELECT VERSION()");
            final String resultSetString = resultSet.getString(1);

            assertThat(resultSetString)
                .as("The database version can be set using a container rule parameter")
                .isEqualTo(dockerImageName.getVersionPart());
        }
    }
}
