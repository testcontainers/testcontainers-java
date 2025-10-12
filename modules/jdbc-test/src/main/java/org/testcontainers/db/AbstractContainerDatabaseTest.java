package org.testcontainers.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.assertj.core.api.Assertions;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Consumer;

import javax.sql.DataSource;

public abstract class AbstractContainerDatabaseTest {

    protected void performQuery(
        final JdbcDatabaseContainer<?> container,
        final String sql,
        final Consumer<ResultSet> consumer
    ) throws SQLException {
        final DataSource ds = getDataSource(container);

        try (
            Connection connection = ds.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql)
        ) {
            resultSet.next();
            consumer.accept(resultSet);
        }
    }

    protected void performSelectOneQuery(final JdbcDatabaseContainer<?> container) throws SQLException {
        performQuery(
            container,
            "SELECT 1",
            resultSet -> {
                Assertions
                    .assertThatNoException()
                    .isThrownBy(() -> {
                        int resultSetInt = resultSet.getInt(1);
                        Assertions.assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                    });
            }
        );
    }

    protected void performSelectFooBarQuery(final JdbcDatabaseContainer<?> container) throws SQLException {
        performQuery(
            container,
            "SELECT foo FROM bar",
            resultSet -> {
                Assertions
                    .assertThatNoException()
                    .isThrownBy(() -> {
                        String firstColumnValue = resultSet.getString(1);
                        Assertions
                            .assertThat(firstColumnValue)
                            .as("Value from init script should equal real value")
                            .isEqualTo("hello world");
                    });
            }
        );
    }

    protected DataSource getDataSource(final JdbcDatabaseContainer<?> container) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(container.getJdbcUrl());
        hikariConfig.setUsername(container.getUsername());
        hikariConfig.setPassword(container.getPassword());
        hikariConfig.setDriverClassName(container.getDriverClassName());
        return new HikariDataSource(hikariConfig);
    }
}
