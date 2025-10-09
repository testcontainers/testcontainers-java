package org.testcontainers.mssqlserver;

import org.junit.jupiter.api.Test;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MSSQLServerContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            MSSQLServerContainer mssqlServer = new MSSQLServerContainer(
                "mcr.microsoft.com/mssql/server:2022-CU20-ubuntu-22.04"
            )
                .acceptLicense()
            // }
        ) {
            mssqlServer.start();
            ResultSet resultSet = performQuery(mssqlServer, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
            assertHasCorrectExposedAndLivenessCheckPorts(mssqlServer);
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            MSSQLServerContainer mssqlServer = new MSSQLServerContainer(MSSQLServerTestImages.MSSQL_SERVER_IMAGE)
                .withUrlParam("integratedSecurity", "false")
                .withUrlParam("applicationName", "MyApp")
        ) {
            mssqlServer.start();

            String jdbcUrl = mssqlServer.getJdbcUrl();
            assertThat(jdbcUrl).contains(";integratedSecurity=false;applicationName=MyApp");
        }
    }

    @Test
    void testSetupDatabase() throws SQLException {
        try (MSSQLServerContainer mssqlServer = new MSSQLServerContainer(MSSQLServerTestImages.MSSQL_SERVER_IMAGE)) {
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

    @Test
    void testSqlServerConnection() throws SQLException {
        try (
            MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer(
                DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04")
            )
                .withPassword("myStrong(!)Password")
        ) {
            mssqlServerContainer.start();

            ResultSet resultSet = performQuery(mssqlServerContainer, mssqlServerContainer.getTestQueryString());
            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(MSSQLServerContainer mssqlServer) {
        assertThat(mssqlServer.getExposedPorts()).containsExactly(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        assertThat(mssqlServer.getLivenessCheckPortNumbers())
            .containsExactly(mssqlServer.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT));
    }
}
