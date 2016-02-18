package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.VirtuosoContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleVirtuosoTest {

    @Rule
    public VirtuosoContainer virtuoso = new VirtuosoContainer();

    @Test
    public void testSimple() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(virtuoso.getJdbcUrl());
        hikariConfig.setUsername(virtuoso.getUsername());
        hikariConfig.setPassword(virtuoso.getPassword());
        hikariConfig.setConnectionTestQuery("SELECT 1");

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT 1");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }
}
