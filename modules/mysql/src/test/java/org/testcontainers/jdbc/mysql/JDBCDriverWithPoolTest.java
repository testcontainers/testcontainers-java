package org.testcontainers.jdbc.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.tomcat.jdbc.pool.PoolProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.jdbc.ContainerDatabaseDriver;
import org.vibur.dbcp.ViburDBCPDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static java.util.Arrays.asList;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * This test belongs in the jdbc module, as it is focused on testing the behaviour of {@link org.testcontainers.containers.JdbcDatabaseContainer}.
 * However, the need to use the {@link org.testcontainers.containers.MySQLContainerProvider} (due to the jdbc:tc:mysql) URL forces it to live here in
 * the mysql module, to avoid circular dependencies.
 * TODO: Move to the jdbc module and either (a) implement a barebones {@link org.testcontainers.containers.JdbcDatabaseContainerProvider} for testing, or (b) refactor into a unit test.
 */
@RunWith(Parameterized.class)
public class JDBCDriverWithPoolTest {

    public static final String URL = "jdbc:tc:mysql:5.7.34://hostname/databasename?TC_INITFUNCTION=org.testcontainers.jdbc.mysql.JDBCDriverWithPoolTest::sampleInitFunction";
    private final DataSource dataSource;

    @Parameterized.Parameters
    public static Iterable<Supplier<DataSource>> dataSourceSuppliers() {
        return asList(
            JDBCDriverWithPoolTest::getTomcatDataSourceWithDriverClassName,
            JDBCDriverWithPoolTest::getTomcatDataSource,
            JDBCDriverWithPoolTest::getHikariDataSourceWithDriverClassName,
            JDBCDriverWithPoolTest::getHikariDataSource,
            JDBCDriverWithPoolTest::getViburDataSourceWithDriverClassName,
            JDBCDriverWithPoolTest::getViburDataSource
        );
    }

    public JDBCDriverWithPoolTest(Supplier<DataSource> dataSourceSupplier) {
        this.dataSource = dataSourceSupplier.get();
    }

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    @Test
    public void testMySQLWithConnectionPoolUsingSameContainer() throws SQLException, InterruptedException {

        // Populate the database with some data in multiple threads, so that multiple connections from the pool will be used
        for (int i = 0; i < 100; i++) {
            executorService.submit(() -> {
                try {
                    new QueryRunner(dataSource).insert("INSERT INTO my_counter (n) VALUES (5)",
                        (ResultSetHandler<Object>) rs -> true);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        }

        // Complete population of the database
        executorService.shutdown();
        executorService.awaitTermination(5, TimeUnit.MINUTES);

        // compare to expected results
        int count = new QueryRunner(dataSource).query("SELECT COUNT(1) FROM my_counter", rs -> {
            rs.next();
            return rs.getInt(1);
        });
        assertEquals("Reuse of a datasource points to the same DB container", 100, count);


        int sum = new QueryRunner(dataSource).query("SELECT SUM(n) FROM my_counter", rs -> {
            rs.next();
            return rs.getInt(1);
        });
        // 100 records * 5 = 500 expected
        assertEquals("Reuse of a datasource points to the same DB container", 500, sum);
    }


    private static DataSource getTomcatDataSourceWithDriverClassName() {
        PoolProperties poolProperties = new PoolProperties();
        poolProperties.setUrl(URL + ";TEST=TOMCAT_WITH_CLASSNAME"); // append a dummy URL element to ensure different DB per test
        poolProperties.setValidationQuery("SELECT 1");
        poolProperties.setMinIdle(3);
        poolProperties.setMaxActive(10);
        poolProperties.setDriverClassName(ContainerDatabaseDriver.class.getName());

        return new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
    }

    private static DataSource getTomcatDataSource() {
        PoolProperties poolProperties = new PoolProperties();
        poolProperties.setUrl(URL + ";TEST=TOMCAT"); // append a dummy URL element to ensure different DB per test
        poolProperties.setValidationQuery("SELECT 1");
        poolProperties.setInitialSize(3);
        poolProperties.setMaxActive(10);

        return new org.apache.tomcat.jdbc.pool.DataSource(poolProperties);
    }

    private static HikariDataSource getHikariDataSourceWithDriverClassName() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(URL + ";TEST=HIKARI_WITH_CLASSNAME"); // append a dummy URL element to ensure different DB per test
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMinimumIdle(3);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setDriverClassName(ContainerDatabaseDriver.class.getName());

        return new HikariDataSource(hikariConfig);
    }

    private static HikariDataSource getHikariDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(URL + ";TEST=HIKARI"); // append a dummy URL element to ensure different DB per test
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setMinimumIdle(3);
        hikariConfig.setMaximumPoolSize(10);

        return new HikariDataSource(hikariConfig);
    }

    private static DataSource getViburDataSourceWithDriverClassName() {
        ViburDBCPDataSource ds = new ViburDBCPDataSource();

        ds.setJdbcUrl(URL + ";TEST=VIBUR_WITH_CLASSNAME");
        ds.setUsername("any");  // Recent versions of Vibur require a username, even though it will not be used
        ds.setPassword("");
        ds.setPoolInitialSize(3);
        ds.setPoolMaxSize(10);
        ds.setTestConnectionQuery("SELECT 1");
        ds.setDriverClassName(ContainerDatabaseDriver.class.getName());

        ds.start();

        return ds;
    }

    private static DataSource getViburDataSource() {
        ViburDBCPDataSource ds = new ViburDBCPDataSource();
        ds.setJdbcUrl(URL + ";TEST=VIBUR");
        ds.setUsername("any");  // Recent versions of Vibur require a username, even though it will not be used
        ds.setPassword("");
        ds.setPoolInitialSize(3);
        ds.setPoolMaxSize(10);
        ds.setTestConnectionQuery("SELECT 1");

        ds.start();

        return ds;
    }

    @SuppressWarnings("SqlNoDataSourceInspection")
    public static void sampleInitFunction(Connection connection) throws SQLException {
        connection.createStatement().execute("CREATE TABLE bar (\n" +
            "  foo VARCHAR(255)\n" +
            ");");
        connection.createStatement().execute("INSERT INTO bar (foo) VALUES ('hello world');");
        connection.createStatement().execute("CREATE TABLE my_counter (\n" +
            "  n INT\n" +
            ");");
    }
}
