package org.testcontainers.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

import javax.sql.DataSource;

public abstract class AbstractContainerDatabaseTest {

    protected ResultSet performQuery(
        JdbcDatabaseContainer<?> container,
        String sql,
        final Consumer<ResultSet> consumer
    ) throws SQLException {
        DataSource ds = getDataSource(container);

        try (Connection ignored = ds.getConnection(); Statement statement = ds.getConnection().createStatement()) {
            statement.execute(sql);
            ResultSet resultSet = statement.getResultSet();
            resultSet.next();
            consumer.accept(resultSet);
        }

        return null;
    }

    protected DataSource getDataSource(JdbcDatabaseContainer<?> container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        return new HikariDataSource(hikariConfig);
    }
}
