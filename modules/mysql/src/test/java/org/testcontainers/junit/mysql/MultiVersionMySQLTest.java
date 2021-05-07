package org.testcontainers.junit.mysql;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.MySQLTestImages.MYSQL_56_IMAGE;
import static org.testcontainers.MySQLTestImages.MYSQL_57_IMAGE;
import static org.testcontainers.MySQLTestImages.MYSQL_80_IMAGE;

@RunWith(Parameterized.class)
public class MultiVersionMySQLTest extends AbstractContainerDatabaseTest {

    @Parameterized.Parameters(name = "{0}")
    public static DockerImageName[] params() {
        return new DockerImageName[]{
            MYSQL_56_IMAGE,
            MYSQL_57_IMAGE,
            MYSQL_80_IMAGE
        };
    }

    @Parameterized.Parameter()
    public DockerImageName dockerImageName;

    @Test
    public void versionCheckTest() throws SQLException {
        try (MySQLContainer<?> mysql = new MySQLContainer<>(dockerImageName)) {
            mysql.start();
            final ResultSet resultSet = performQuery(mysql, "SELECT VERSION()");
            final String resultSetString = resultSet.getString(1);

            assertEquals("The database version can be set using a container rule parameter", dockerImageName.getVersionPart(), resultSetString);
        }
    }
}
