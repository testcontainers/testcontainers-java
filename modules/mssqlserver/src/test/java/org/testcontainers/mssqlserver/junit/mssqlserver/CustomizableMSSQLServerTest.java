package org.testcontainers.mssqlserver.junit.mssqlserver;

import org.junit.Test;
import org.testcontainers.jdbctest.db.AbstractContainerDatabaseTest;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

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

            ResultSet resultSet = performQuery(mssqlServerContainer, mssqlServerContainer.getTestQueryString());
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }
}
