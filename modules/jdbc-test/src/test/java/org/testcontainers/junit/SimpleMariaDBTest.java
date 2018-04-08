package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.NonNull;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.testcontainers.containers.MariaDBContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.junit.Assume.assumeFalse;


/**
 * @author Miguel Gonzalez Sanchez
 */
public class SimpleMariaDBTest {

    @Test
    public void testSimple() throws SQLException {
        MariaDBContainer mariadb = new MariaDBContainer();
        mariadb.start();

        try {
            ResultSet resultSet = performQuery(mariadb, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        } finally {
            mariadb.stop();
        }
    }

    @Test
    public void testSpecificVersion() throws SQLException {
        MariaDBContainer mariadbOldVersion = new MariaDBContainer("mariadb:5.5.51");
        mariadbOldVersion.start();

        try {
            ResultSet resultSet = performQuery(mariadbOldVersion, "SELECT VERSION()");
            String resultSetString = resultSet.getString(1);

            assertTrue("The database version can be set using a container rule parameter", resultSetString.startsWith("5.5.51"));
        } finally {
            mariadbOldVersion.stop();
        }
    }

    @Test
    public void testMariaDBWithCustomIniFile() throws SQLException {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        MariaDBContainer mariadbCustomConfig = new MariaDBContainer("mariadb:10.1.16")
                                                .withConfigurationOverride("somepath/mariadb_conf_override");
        mariadbCustomConfig.start();

        try {
            ResultSet resultSet = performQuery(mariadbCustomConfig, "SELECT @@GLOBAL.innodb_file_format");
            String result = resultSet.getString(1);

            assertEquals("The InnoDB file format has been set by the ini file content", "Barracuda", result);
        } finally {
            mariadbCustomConfig.stop();
        }
    }

    @NonNull
    protected ResultSet performQuery(MariaDBContainer containerRule, String sql) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(containerRule.getJdbcUrl());
        hikariConfig.setUsername(containerRule.getUsername());
        hikariConfig.setPassword(containerRule.getPassword());

        HikariDataSource ds = new HikariDataSource(hikariConfig);
        Statement statement = ds.getConnection().createStatement();
        statement.execute(sql);
        ResultSet resultSet = statement.getResultSet();

        resultSet.next();
        return resultSet;
    }
}
