package org.testcontainers.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import static org.junit.Assert.assertTrue;

public abstract class AbstractContainerDatabaseTest {

    /**
     *
     * @param container
     * @param sql
     * @return
     * @throws SQLException
     * @deprecated use {@link #assertQuery(JdbcDatabaseContainer, String, ResultSetConsumer)} instead.
     */
    protected ResultSet performQuery(JdbcDatabaseContainer<?> container, String sql) throws SQLException {
        DataSource ds = getDataSource(container);
        Statement statement = ds.getConnection().createStatement();
        statement.execute(sql);
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        return resultSet;
    }

    protected void assertQuery(JdbcDatabaseContainer<?> container, String sql, ResultSetConsumer assertion)
        throws SQLException {
        try (
            HikariDataSource ds = getDataSource0(container);
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {
            assertTrue(rs.next());
            assertion.accept(rs);
        }
    }

    protected DataSource getDataSource(JdbcDatabaseContainer<?> container) {
        return getDataSource0(container);
    }

    private HikariDataSource getDataSource0(JdbcDatabaseContainer<?> container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        return new HikariDataSource(hikariConfig);
    }

    @FunctionalInterface
    public interface ResultSetConsumer {
        void accept(ResultSet resultSet) throws SQLException;
    }
}
