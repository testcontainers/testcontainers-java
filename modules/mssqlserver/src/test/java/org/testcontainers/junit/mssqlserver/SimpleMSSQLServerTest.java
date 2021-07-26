package org.testcontainers.junit.mssqlserver;

import org.junit.Test;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.MSSQLServerTestImages.MSSQL_SERVER_IMAGE;

public class SimpleMSSQLServerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (MSSQLServerContainer<?> mssqlServer = new MSSQLServerContainer<>(MSSQL_SERVER_IMAGE)) {
            mssqlServer.start();
            ResultSet resultSet = performQuery(mssqlServer, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (MSSQLServerContainer<?> mssqlServer = new MSSQLServerContainer<>(MSSQL_SERVER_IMAGE)
            .withUrlParam("integratedSecurity", "false")
            .withUrlParam("applicationName", "MyApp")) {

            mssqlServer.start();

            String jdbcUrl = mssqlServer.getJdbcUrl();
            assertThat(jdbcUrl, containsString(";integratedSecurity=false;applicationName=MyApp"));
        }
    }


    @Test
    public void testSetupDatabase() throws SQLException {
        try (MSSQLServerContainer<?> mssqlServer = new MSSQLServerContainer<>(MSSQL_SERVER_IMAGE)) {
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
            assertEquals("A basic SELECT query succeeds", 3, resultSetInt);
        }
    }
}
