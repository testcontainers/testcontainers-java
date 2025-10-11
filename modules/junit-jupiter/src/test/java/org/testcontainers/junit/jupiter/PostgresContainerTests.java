package org.testcontainers.junit.jupiter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class PostgresContainerTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>(
        JUnitJupiterTestImages.POSTGRES_IMAGE
    )
        .withDatabaseName("foo")
        .withUsername("foo")
        .withPassword("secret");

    @Test
    void waits_until_postgres_accepts_jdbc_connections() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(POSTGRE_SQL_CONTAINER.getJdbcUrl());
        hikariConfig.setUsername("foo");
        hikariConfig.setPassword("secret");

        try (
            HikariDataSource ds = new HikariDataSource(hikariConfig);
            Connection conn = ds.getConnection();
            Statement statement = conn.createStatement()
        ) {
            statement.execute("SELECT 1");
            try (ResultSet resultSet = statement.getResultSet()) {
                resultSet.next();
                int resultSetInt = resultSet.getInt(1);
                assertThat(resultSetInt).isEqualTo(1);
            }
        }
    }
}
