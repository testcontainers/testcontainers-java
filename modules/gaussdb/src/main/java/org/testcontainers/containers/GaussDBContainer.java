package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Testcontainers implementation for GaussDB.
 * <p>
 * Supported images: {@code opengauss/opengauss}
 * <p>
 * Exposed ports: 8000
 */
public class GaussDBContainer<SELF extends GaussDBContainer<SELF>> extends JdbcDatabaseContainer<SELF> {

    public static final String NAME = "gaussdb";

    public static final String IMAGE = "opengauss/opengauss";

    public static final String DEFAULT_TAG = "7.0.0-RC1.B023";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(IMAGE);

    public static final Integer GaussDB_PORT = 8000;

    public static final String DEFAULT_USER_NAME = "test";

    // At least one uppercase, lowercase, numeric, special character, and password length(8).
    public static final String DEFAULT_PASSWORD = "Test@123";

    private String databaseName = "gaussdb";

    private String username = DEFAULT_USER_NAME;

    private String password = DEFAULT_PASSWORD;

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
        setWaitStrategy(new WaitStrategy() {
            @Override
            public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
                Wait.forListeningPort().waitUntilReady(waitStrategyTarget);
                try {
                    // Open Gauss will set up users and password when ports are ready.
                    Wait.forLogMessage(".*gs_ctl stopped.*", 1).waitUntilReady(waitStrategyTarget);
                    // Not enough and no idea
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public WaitStrategy withStartupTimeout(Duration duration) {
                return GenericContainer.DEFAULT_WAIT_STRATEGY.withStartupTimeout(duration);
            }
        });
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
        // Disable GaussDB driver use of java.util.logging to reduce noise at startup time
        withUrlParam("loggerLevel", "OFF");
        addExposedPorts(GaussDB_PORT);
        addEnv("GS_DB", databaseName);
        addEnv("GS_PORT", String.valueOf(GaussDB_PORT));
        addEnv("GS_USERNAME", username);
        addEnv("GS_PASSWORD", password);
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
