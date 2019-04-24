package org.testcontainers.junit;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PgroutingContainerProvider;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SimplePgroutingSQLTest {

    @Test
    public void testPgRoutingVersion() throws SQLException {
        try (JdbcDatabaseContainer container = new PgroutingContainerProvider().newInstance()) {
            container.start();

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(container.getJdbcUrl());
            hikariConfig.setUsername(container.getUsername());
            hikariConfig.setPassword(container.getPassword());

            HikariDataSource ds = new HikariDataSource(hikariConfig);
            Statement statement = ds.getConnection().createStatement();
            statement.execute("select pgr_version()");
            ResultSet resultSet = statement.getResultSet();
            resultSet.next();
        }
    }
}
