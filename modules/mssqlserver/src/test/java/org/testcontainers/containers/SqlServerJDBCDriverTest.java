package org.testcontainers.containers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SqlServerJDBCDriverTest {

    @Test
    public void testMsSqlServerConnection() throws Exception {
        HikariDataSource dataSource = getDataSource("jdbc:tc:sqlserver:2017-CU12://somehost:someport;databaseName=test");
        new QueryRunner(dataSource).query("SELECT 1", (ResultSetHandler<Object>) rs -> {
            rs.next();
            int resultSetInt = rs.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
            return true;
        });
        dataSource.close();
    }

    private HikariDataSource getDataSource(String jdbcUrl) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(1);

        return new HikariDataSource(hikariConfig);
    }
}
