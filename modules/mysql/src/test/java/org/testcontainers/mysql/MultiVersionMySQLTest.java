package org.testcontainers.mysql;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class MultiVersionMySQLTest extends AbstractContainerDatabaseTest {

    public static DockerImageName[] params() {
        return new DockerImageName[] {
            MySQLTestImages.MYSQL_57_IMAGE,
            MySQLTestImages.MYSQL_80_IMAGE,
            MySQLTestImages.MYSQL_INNOVATION_IMAGE,
            MySQLTestImages.MYSQL_93_IMAGE,
        };
    }

    @ParameterizedTest
    @MethodSource("params")
    void versionCheckTest(DockerImageName dockerImageName) throws SQLException {
        try (MySQLContainer mysql = new MySQLContainer(dockerImageName)) {
            mysql.start();
            performQuery(
                mysql,
                "SELECT VERSION()",
                resultSet -> {
                    Assertions
                        .assertThatNoException()
                        .isThrownBy(() -> {
                            final String resultSetString = resultSet.getString(1);
                            assertThat(resultSetString)
                                .as("The database version can be set using a container rule parameter")
                                .isEqualTo(dockerImageName.getVersionPart());
                        });
                }
            );
        }
    }
}
