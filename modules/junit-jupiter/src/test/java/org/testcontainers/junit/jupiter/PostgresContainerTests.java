package org.testcontainers.junit.jupiter;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.testcontainers.junit.jupiter.JUnitJupiterTestImages.POSTGRES_IMAGE;

@Testcontainers
class PostgresContainerTests {

    @Container
    private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>(POSTGRES_IMAGE)
            .withDatabaseName("foo")
            .withUsername("foo")
            .withPassword("secret");

    @Test
    void waits_until_postgres_accepts_jdbc_connections() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(POSTGRE_SQL_CONTAINER.getJdbcUrl());
        hikariConfig.setUsername("foo");
        hikariConfig.setPassword("secret");

        try (HikariDataSource ds = new HikariDataSource(hikariConfig)) {
            Statement statement = ds.getConnection().createStatement();
            statement.execute("SELECT 1");
            ResultSet resultSet = statement.getResultSet();
            resultSet.next();

            int resultSetInt = resultSet.getInt(1);
            assertEquals(1, resultSetInt);
        }
    }

}
