package org.testcontainers.containers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Rule;
import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.fail;

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
        hikariConfig.setJdbcUrl("jdbc:sqlserver://localhost:" +
            mssqlServerContainer.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT));
        hikariConfig.setUsername("SA");
        hikariConfig.setPassword(STRONG_PASSWORD);

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute(mssqlServerContainer.getTestQueryString());
        ResultSet resultSet = statement.getResultSet();
        if(resultSet.next()){
            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        } else {
            fail("No results returned from query");
        }

    }
}
