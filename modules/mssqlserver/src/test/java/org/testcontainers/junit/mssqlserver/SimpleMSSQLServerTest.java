package org.testcontainers.junit.mssqlserver;

import org.junit.Test;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleMSSQLServerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (
            MSSQLServerContainer<?> mssqlServer = new MSSQLServerContainer<>(MSSQLServerTestImages.MSSQL_SERVER_IMAGE)
        ) {
            mssqlServer.start();
            ResultSet resultSet = performQuery(mssqlServer, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(mssqlServer);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            MSSQLServerContainer<?> mssqlServer = new MSSQLServerContainer<>(MSSQLServerTestImages.MSSQL_SERVER_IMAGE)
                .withUrlParam("integratedSecurity", "false")
                .withUrlParam("applicationName", "MyApp")
        ) {
            mssqlServer.start();

            String jdbcUrl = mssqlServer.getJdbcUrl();
            assertThat(jdbcUrl).contains(";integratedSecurity=false;applicationName=MyApp");
        }
    }

    @Test
    public void testSetupDatabase() throws SQLException {
        try (
            MSSQLServerContainer<?> mssqlServer = new MSSQLServerContainer<>(MSSQLServerTestImages.MSSQL_SERVER_IMAGE)
        ) {
            mssqlServer.start();
            DataSource ds = getDataSource(mssqlServer);
            Statement statement = ds.getConnection().createStatement();
            statement.executeUpdate("CREATE DATABASE [test];");
            statement = ds.getConnection().createStatement();
            statement.executeUpdate("CREATE TABLE [test].[dbo].[Foo](ID INT PRIMARY KEY);");
            statement = ds.getConnection().createStatement();
            statement.executeUpdate("INSERT INTO [test].[dbo].[Foo] (ID) VALUES (3);");
            statement = ds.getConnection().createStatement();
            statement.execute("SELECT * FROM [test].[dbo].[Foo];");
            ResultSet resultSet = statement.getResultSet();

            resultSet.next();
            int resultSetInt = resultSet.getInt("ID");
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(3);
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(MSSQLServerContainer<?> mssqlServer) {
        assertThat(mssqlServer.getExposedPorts()).containsExactly(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        assertThat(mssqlServer.getLivenessCheckPortNumbers())
            .containsExactly(mssqlServer.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT));
    }
}
