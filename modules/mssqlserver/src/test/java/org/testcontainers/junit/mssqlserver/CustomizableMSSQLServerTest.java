package org.testcontainers.junit.mssqlserver;

import org.junit.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class CustomizableMSSQLServerTest extends AbstractContainerDatabaseTest {

    private static final String STRONG_PASSWORD = "myStrong(!)Password";

    @Test
    public void testSqlServerConnection() throws SQLException {
        try (
            MSSQLServerContainer<?> mssqlServerContainer = new MSSQLServerContainer<>(
                DockerImageName.parse("mcr.microsoft.com/mssql/server:2017-CU12")
            )
                .withPassword(STRONG_PASSWORD)
        ) {
            mssqlServerContainer.start();

            assertQuery(
                mssqlServerContainer,
                mssqlServerContainer.getTestQueryString(),
                rs -> {
                    int resultSetInt = rs.getInt(1);
                    assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
                }
            );
        }
    }
}
