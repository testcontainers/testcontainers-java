package org.testcontainers.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

public abstract class AbstractContainerDatabaseTest {

    protected ResultSet performQuery(JdbcDatabaseContainer<?> container, String sql) throws SQLException {
        try (HikariDataSource ds = (HikariDataSource) getDataSource(container)) {
            Statement statement = ds.getConnection().createStatement();
            statement.execute(sql);
            ResultSet resultSet = statement.getResultSet();

            resultSet.next();
            return resultSet;
        }
    }

    protected final DataSource getDataSource(JdbcDatabaseContainer<?> container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        return new HikariDataSource(hikariConfig);
    }
}
