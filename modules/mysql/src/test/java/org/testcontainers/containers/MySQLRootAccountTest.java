package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Slf4j
@ParameterizedClass
@MethodSource("params")
public class MySQLRootAccountTest {

    public static DockerImageName[] params() {
        return new DockerImageName[] {
            MySQLTestImages.MYSQL_57_IMAGE,
            MySQLTestImages.MYSQL_80_IMAGE,
            MySQLTestImages.MYSQL_INNOVATION_IMAGE,
        };
    }

    @Parameter(0)
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
