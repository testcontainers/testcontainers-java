package org.testcontainers.junit.mssqlserver;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
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
                DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04")
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
