package org.testcontainers.containers.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.junit.Ignore;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class OracleJDBCDriverTest {

    @Test
    public void testOracleWithNoSpecifiedVersion() throws SQLException {
        performSimpleTest("jdbc:tc:oracle://hostname/databasename");
    }


    private void performSimpleTest(String jdbcUrl) throws SQLException {
        HikariDataSource dataSource = getDataSource(jdbcUrl, 1);
        new QueryRunner(dataSource).query("SELECT 1 FROM dual", new ResultSetHandler<Object>() {
            @Override
            public Object handle(ResultSet rs) throws SQLException {
                rs.next();
                int resultSetInt = rs.getInt(1);
                assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
                return true;
            }
        });
        dataSource.close();
    }

    private HikariDataSource getDataSource(String jdbcUrl, int poolSize) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(jdbcUrl);
        hikariConfig.setConnectionTestQuery("SELECT 1 FROM dual");
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(poolSize);

        return new HikariDataSource(hikariConfig);
    }
}
