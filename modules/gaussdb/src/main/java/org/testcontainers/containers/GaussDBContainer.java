package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Testcontainers implementation for PostgreSQL.
 * <p>
 * Supported images: {@code postgres}, {@code pgvector/pgvector}
 * <p>
 * Exposed ports: 5432
 */
public class GaussDBContainer<SELF extends GaussDBContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "gaussdb";

    public static final String IMAGE = "opengauss/opengauss";

    public static final String DEFAULT_TAG = "latest";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("opengauss/opengauss").asCompatibleSubstituteFor("gaussdb");

    public static final Integer GaussDB_PORT = 5432;

    static final String DEFAULT_USER = "gaussdb";

    static final String DEFAULT_PASSWORD = "Enmo@123";

    private String databaseName = "postgres";

    private String username = "gaussdb";

    private String password = "Enmo@123";

    private static final String FSYNC_OFF_OPTION = "fsync=off";

    /**
     * @deprecated use {@link #GaussDBContainer(DockerImageName)} or {@link #GaussDBContainer(String)} instead
     */
    @Deprecated
    public GaussDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public GaussDBContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public GaussDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withEnv("GS_PASSWORD", "Enmo@123")
            .withDatabaseName("postgres");
        // Comment out the test error code for the time being
//        this.waitStrategy
//            =
//            new LogMessageWaitStrategy()
//                .withRegEx(".*can not read GAUSS_WARNING_TYPE env.*\\s")
//                .withTimes(1)
//                .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS));
//        this.setCommand("-c", FSYNC_OFF_OPTION);

        addExposedPort(GaussDB_PORT);
    }

    /**
     * @return the ports on which to check if the container is ready
     * @deprecated use {@link #getLivenessCheckPortNumbers()} instead
     */
    @NotNull
    @Override
    @Deprecated
    protected Set<Integer> getLivenessCheckPorts() {
        return super.getLivenessCheckPorts();
    }

    @Override
    protected void configure() {
        // Disable Postgres driver use of java.util.logging to reduce noise at startup time
        withUrlParam("loggerLevel", "OFF");
        addEnv("POSTGRES_DB", databaseName);
        addEnv("POSTGRES_USER", username);
        addEnv("POSTGRES_PASSWORD", password);
    }

    @Override
    public String getDriverClassName() {
        return "com.huawei.gaussdb.jdbc.Driver";
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return (
            "jdbc:gaussdb://" +
            getHost() +
            ":" +
            getMappedPort(GaussDB_PORT) +
            "/" +
            databaseName +
            additionalUrlParams
        );
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public SELF withPassword(final String password) {
        this.password = password;
        return self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
