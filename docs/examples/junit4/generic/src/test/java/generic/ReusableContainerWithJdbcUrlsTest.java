package generic;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertTrue;

public class ReusableContainerWithJdbcUrlsTest {

    private static final Logger logger = LoggerFactory.getLogger(ReusableContainerWithJdbcUrlsTest.class);

    // jdbc_init {
    private static GenericContainer<?> database;

    @BeforeClass
    public static void setUp() {
        database = new GenericContainer<>("postgres:9.6.17")
            .withReuse(true);
        database.start();
    }

    @Test
    public void testKafkaContainer() throws SQLException {
        assertTrue(database.isRunning());

        Instant startedAt = Instant.now();
        Connection connection = DriverManager.getConnection(
            "jdbc:tc:postgresql:9.6.17:///?TC_REUSABLE=true"
        );

        boolean execute = connection.createStatement().execute("SELECT 1");

        logger.info("Total test time: {}", Duration.between(startedAt, Instant.now()));

        assertTrue(execute);
    }
    // }
}
