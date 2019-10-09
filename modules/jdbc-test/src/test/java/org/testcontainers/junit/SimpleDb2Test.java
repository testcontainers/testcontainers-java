package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.Db2Container;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;


public class SimpleDb2Test {

    @Rule
    public Db2Container db2 = new Db2Container()
        .acceptLicense();

    @Test
    public void testSimple() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(db2.getJdbcUrl());
        hikariConfig.setUsername(db2.getUsername());
        hikariConfig.setPassword(db2.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute("SELECT 1 FROM SYSIBM.SYSDUMMY1");
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }

    @Test
    public void testCreateJdbcUrlWithUrlParam() {
        JdbcDatabaseContainer container = new Db2Container()
            .withUrlParam("traceLevel", "TRACE_ALL");
        try {
            container.start();
            String actual = container.getJdbcUrl();
            assertTrue("Jdbc url should contains params", actual.contains(":traceLevel=TRACE_ALL"));
        } finally {
            container.stop();
        }
    }
}
