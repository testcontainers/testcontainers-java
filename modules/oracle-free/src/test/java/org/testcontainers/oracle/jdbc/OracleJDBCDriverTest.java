package org.testcontainers.oracle.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class OracleJDBCDriverTest {

    @Test
    void testOracleWithNoSpecifiedVersion() throws SQLException {
        try (HikariDataSource dataSource = getDataSource()) {
            new QueryRunner(dataSource)
                .query(
                    "SELECT 1 FROM dual",
                    (ResultSetHandler<Object>) rs -> {
                        rs.next();
                        int resultSetInt = rs.getInt(1);
                        assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                        return true;
                    }
                );
        }
    }

    private HikariDataSource getDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:tc:oracle://hostname/databasename");
        hikariConfig.setConnectionTestQuery("SELECT 1 FROM dual");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(1);

        return new HikariDataSource(hikariConfig);
    }
}
