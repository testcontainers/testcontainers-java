package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Rule;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.testpackage.VisibleAssertions.assertEquals;
import static org.testpackage.VisibleAssertions.assertTrue;

/**
 * @author richardnorth
 */
public class SimpleMySQLTest {

    @Rule
    public MySQLContainerRule mysql = new MySQLContainerRule();

    @Rule
    public MySQLContainerRule mysqlOldVersion = new MySQLContainerRule("mysql:5.5");

    @Test
    public void testSimple() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(mysql.getJdbcUrl());
        hikariConfig.setUsername(mysql.getUsername());
        hikariConfig.setPassword(mysql.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT 1");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(mysqlOldVersion.getJdbcUrl());
        hikariConfig.setUsername(mysqlOldVersion.getUsername());
        hikariConfig.setPassword(mysqlOldVersion.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT VERSION()");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        String resultSetString = resultSet.getString(1);
        assertTrue("The database version can be set using a container rule parameter", resultSetString.startsWith("5.5"));
    }
}
