package org.testcontainers.mysql;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

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
            executeSelectVersionQuery(mysql, dockerImageName.getVersionPart());
        }
    }
}
