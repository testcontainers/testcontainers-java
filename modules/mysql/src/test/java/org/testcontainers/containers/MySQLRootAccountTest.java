package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.function.Consumer;

@Slf4j
class MySQLRootAccountTest {

    public static DockerImageName[] params() {
        return new DockerImageName[] {
            MySQLTestImages.MYSQL_57_IMAGE,
            MySQLTestImages.MYSQL_80_IMAGE,
            MySQLTestImages.MYSQL_INNOVATION_IMAGE,
            MySQLTestImages.MYSQL_93_IMAGE,
        };
    }

    @ParameterizedTest
    @MethodSource("params")
    void testRootAccountUsageWithDefaultPassword(DockerImageName image) throws SQLException {
        testWithDB(image, db -> db.withUsername("root"));
    }

    @ParameterizedTest
    @MethodSource("params")
    void testRootAccountUsageWithEmptyPassword(DockerImageName image) throws SQLException {
        testWithDB(image, db -> db.withUsername("root").withPassword(""));
    }

    @ParameterizedTest
    @MethodSource("params")
    void testRootAccountUsageWithCustomPassword(DockerImageName image) throws SQLException {
        testWithDB(image, db -> db.withUsername("root").withPassword("not-default"));
    }

    private void testWithDB(final DockerImageName image, final Consumer<MySQLContainer<?>> consumer)
        throws SQLException {
        try (
            MySQLContainer<?> db = new MySQLContainer<>(image).withLogConsumer(new Slf4jLogConsumer(log));
            Connection connection = DriverManager.getConnection(db.getJdbcUrl(), db.getUsername(), db.getPassword())
        ) {
            consumer.accept(db);
            db.start();
            connection.createStatement().execute("SELECT 1");
            connection.createStatement().execute("set sql_log_bin=0"); // requires root
        }
    }
}
