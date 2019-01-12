package org.testcontainers.containers;

import org.testcontainers.containers.wait.LogMessageWaitStrategy;
import org.testcontainers.r2dbc.R2dbcConnectionParams;
import org.testcontainers.r2dbc.R2dbcDatabaseContainer;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

// The container should expose the R2dbc connection (factory?) to the test so that we can construct a driver instance outside of it.
public class R2dbcPostgreSQLContainer<SELF extends R2dbcPostgreSQLContainer<SELF>> extends R2dbcDatabaseContainer<SELF> {
    public static final String NAME = "postgresql";
    public static final String IMAGE = "postgres";
    public static final String DEFAULT_TAG = "9.6.8";

    public static final Integer POSTGRESQL_PORT = 5432;
    private String databaseName = "test";
    private String username = "test";
    private String password = "test";
    private String host = "localhost";

    @Override
    protected void configure() {
        addExposedPort(POSTGRESQL_PORT);
        addEnv("POSTGRES_DB", databaseName);
        addEnv("POSTGRES_USER", username);
        addEnv("POSTGRES_HOST", host);
        addEnv("POSTGRES_PASSWORD", password);
        setCommand("postgres");
    }

    public R2dbcPostgreSQLContainer() {
        this(IMAGE + ":" + DEFAULT_TAG);
    }

    public R2dbcPostgreSQLContainer(final String dockerImageName) {
        super(dockerImageName);
        this.waitStrategy = new LogMessageWaitStrategy()
            .withRegEx(".*database system is ready to accept connections.*\\s")
            .withTimes(2)
            .withStartupTimeout(Duration.of(60, SECONDS));
    }

    @Override
    public R2dbcConnectionParams getR2dbcConnectionParams(){
        return R2dbcConnectionParams.builder()
            .host(host)
            .database(databaseName)
            .username(username)
            .password(password)
            .port(getMappedPort(POSTGRESQL_PORT))
            .build();
    }

    @Override
    protected String getTestQueryString() {
        return "SELECT 1";
    }
}
