package org.testcontainers.containers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Rule;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * @author Enrico Costanzi
 */
public class MSSQLServerContainerTest {

    private static final String STRONG_PASSWORD = "myStrong(!)Password";

    @Rule
    public MSSQLServerContainer mssqlServerContainer = new MSSQLServerContainer()
        .withPassword(STRONG_PASSWORD);

    @Test
    public void testSqlServerConnection() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlserver://localhost:" + mssqlServerContainer.getMappedPort(1433));
        hikariConfig.setUsername("SA");
        hikariConfig.setPassword(STRONG_PASSWORD);

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT 1");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        
    }
}
