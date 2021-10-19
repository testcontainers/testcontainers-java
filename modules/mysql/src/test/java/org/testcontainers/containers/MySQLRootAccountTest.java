package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
@RunWith(Parameterized.class)
public class MySQLRootAccountTest {

    @Parameterized.Parameters(name = "{0}")
    public static DockerImageName[] params() {
        return new DockerImageName[]{
            MySQLTestImages.MYSQL_80_IMAGE,
            MySQLTestImages.MYSQL_57_IMAGE
        };
    }

    @Parameterized.Parameter()
    public DockerImageName image;

    @Test
    public void testRootAccountUsageWithDefaultPassword() throws SQLException {
        testWithDB(new MySQLContainer<>(image).withUsername("root"));
    }

    @Test
    public void testRootAccountUsageWithEmptyPassword() throws SQLException {
        testWithDB(new MySQLContainer<>(image).withUsername("root").withPassword(""));
    }

    @Test
    public void testRootAccountUsageWithCustomPassword() throws SQLException {
        testWithDB(new MySQLContainer<>(image).withUsername("root").withPassword("not-default"));
    }

    private void testWithDB(MySQLContainer<?> db) throws SQLException {
        try {
            db.withLogConsumer(new Slf4jLogConsumer(log)).start();
            Connection connection = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword());
            connection.createStatement().execute("SELECT 1");
            connection.createStatement().execute("set sql_log_bin=0"); // requires root
        } finally {
            db.close();
        }
    }
}
