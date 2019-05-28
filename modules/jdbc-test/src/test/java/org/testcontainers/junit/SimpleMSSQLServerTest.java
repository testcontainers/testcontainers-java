package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.MSSQLServerContainer;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * @author Stefan Hufschmidt
 */
public class SimpleMSSQLServerTest extends AbstractContainerDatabaseTest {

    @Rule
    public MSSQLServerContainer mssqlServer = new MSSQLServerContainer();

    @Test
    public void testSimple() throws SQLException {
        ResultSet resultSet = performQuery(mssqlServer, "SELECT 1");

        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }

    @Test
    public void testSetupDatabase() throws SQLException {
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
