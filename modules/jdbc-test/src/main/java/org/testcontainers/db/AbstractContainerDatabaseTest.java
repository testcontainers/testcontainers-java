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

    protected void executeQuery(
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

    protected void executeSelectOneQuery(final JdbcDatabaseContainer<?> container) throws SQLException {
        executeSelectOneQuery(container, "SELECT 1");
    }

    protected void executeSelectOneQuery(final JdbcDatabaseContainer<?> container, final String sql)
        throws SQLException {
        executeQuery(
            container,
            sql,
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

    protected void executeSelectFooBarQuery(final JdbcDatabaseContainer<?> container) throws SQLException {
        executeQuery(
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

    protected void executeSelectMaxConnectionsQuery(
        final JdbcDatabaseContainer<?> container,
        final String expectedMaxConnections
    ) throws SQLException {
        executeQuery(
            container,
            "SELECT current_setting('max_connections')",
            resultSet -> {
                Assertions
                    .assertThatNoException()
                    .isThrownBy(() -> {
                        String result = resultSet.getString(1);
                        Assertions
                            .assertThat(result)
                            .as("max_connections should be overridden")
                            .isEqualTo(expectedMaxConnections);
                    });
            }
        );
    }

    protected void executeSelectVersionQuery(final JdbcDatabaseContainer<?> container, final String expectedVersion)
        throws SQLException {
        executeQuery(
            container,
            "SELECT VERSION()",
            resultSet -> {
                Assertions
                    .assertThatNoException()
                    .isThrownBy(() -> {
                        String resultSetString = resultSet.getString(1);
                        Assertions
                            .assertThat(resultSetString)
                            .as("The database version can be set using a container rule parameter")
                            .startsWith(expectedVersion);
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
