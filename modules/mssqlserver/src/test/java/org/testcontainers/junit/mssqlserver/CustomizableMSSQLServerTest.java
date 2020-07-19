package org.testcontainers.junit.mssqlserver;

import org.junit.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class CustomizableMSSQLServerTest extends AbstractContainerDatabaseTest {

    private static final String STRONG_PASSWORD = "myStrong(!)Password";

    @Test
    public void testSqlServerConnection() throws SQLException {
        try (MSSQLServerContainer<?> mssqlServerContainer = new MSSQLServerContainer<>(DockerImageName.parse("mcr.microsoft.com/mssql/server:2017-CU12"))
            .withPassword(STRONG_PASSWORD)) {

            mssqlServerContainer.start();

            ResultSet resultSet = performQuery(mssqlServerContainer, mssqlServerContainer.getTestQueryString());
            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
