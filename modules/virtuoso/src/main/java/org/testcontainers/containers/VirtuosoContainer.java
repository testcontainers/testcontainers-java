package org.testcontainers.containers;

import org.rnorth.ducttape.inconsistents.Inconsistents;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class VirtuosoContainer<SELF extends VirtuosoContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "virtuoso";
    public static final String IMAGE = "tenforce/virtuoso";
    public static final Integer JDBC_PORT = 1111;
    public static final Integer SPARQL_SERVICE_PORT = 8890;
    private static final RateLimiter LIVENESS_RATE_LIMITER = RateLimiterBuilder.newBuilder()
            .withConstantThroughput()
            .withRate(1, TimeUnit.SECONDS)
            .build();

    public VirtuosoContainer() {
        super(IMAGE + ":1.0.0-virtuoso7.2.2");
    }

    public VirtuosoContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        addExposedPort(JDBC_PORT);
        addExposedPort(SPARQL_SERVICE_PORT);
        addEnv("DBA_PASSWORD", getPassword());
        addEnv("SPARQL_UPDATE", "true");
        addEnv("DEFAULT_GRAPH", "http://localhost:8890/DAV");
        addExposedPorts(JDBC_PORT, SPARQL_SERVICE_PORT);
    }

    @Override
    protected String getDriverClassName() {
        return "virtuoso.jdbc4.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:virtuoso://" + getContainerIpAddress() + ":" + getMappedPort(JDBC_PORT);
    }

    public String getSparqlUrl() {
        return "http://" + getContainerIpAddress() + ":" + getMappedPort(SPARQL_SERVICE_PORT) + "/sparql";
    }

    @Override
    public String getUsername() {
        return "dba";
    }

    @Override
    public String getPassword() {
        return "myDbaPassword";
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(JDBC_PORT);
    }

    @Override
    protected void waitUntilContainerStarted() {
        // Repeatedly try and open a connection to the DB and execute a test query

        logger().info("Waiting for database connection to become available at {} using query '{}'", getJdbcUrl(), getTestQueryString());

        // Wait for consecutive JDBC connection successes over a period of time. The Virtuoso container seems
        //  to initially return a connection that fails on subsequent attempts, so wait for a consistently stable connection
        Inconsistents.retryUntilConsistent(5, 120, TimeUnit.SECONDS, () -> {
            //noinspection CodeBlock2Expr
            return LIVENESS_RATE_LIMITER.getWhenReady(() -> {
                if (!isRunning()) {
                    throw new ContainerLaunchException("Container failed to start");
                }

                try {
                    Connection connection = createConnection("");

                    boolean success = connection.createStatement().execute(this.getTestQueryString());

                    if (success) {
                        logger().info("Obtained a connection to container ({})", this.getJdbcUrl());
                        return true;
                    } else {
                        throw new SQLException("Failed to execute test query");
                    }
                } catch (SQLException e) {
                    throw new ContainerLaunchException(e.getMessage());
                }
            });
        });
    }
}
