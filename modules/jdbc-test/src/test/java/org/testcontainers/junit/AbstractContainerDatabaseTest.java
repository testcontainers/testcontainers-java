package org.testcontainers.junit;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.After;
import org.testcontainers.containers.JdbcDatabaseContainer;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

abstract class AbstractContainerDatabaseTest {

    private final Set<HikariDataSource> datasourcesForCleanup = new HashSet<>();

    protected void validateCreateDataSource(JdbcDatabaseContainer container) throws SQLException {
        DataSource dataSource = container.createDataSource();
        Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT 1");
        ResultSet resultSet = ps.executeQuery();
        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals("createDataSource query succeeds", 1, resultSetInt);
        resultSet.close();
        ps.close();
        conn.close();
    }

    ResultSet performQuery(JdbcDatabaseContainer container, String sql) throws SQLException {
        DataSource ds = container.createDataSource();
        Statement statement = ds.getConnection().createStatement();
        statement.execute(sql);
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        return resultSet;
    }

    @After
    public void teardown() {
        datasourcesForCleanup.forEach(HikariDataSource::close);
    }
}
