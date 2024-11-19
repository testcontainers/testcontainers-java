package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Testcontainers implementation for CockroachDB.
 * <p>
 * Supported image: {@code cockroachdb/cockroach}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 26257</li>
 *     <li>Console: 8080</li>
 * </ul>
 */
public class CockroachContainer extends JdbcDatabaseContainer<CockroachContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("cockroachdb/cockroach");

    private static final String DEFAULT_TAG = "v19.2.11";

    public static final String NAME = "cockroach";

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    @Deprecated
    public static final String IMAGE_TAG = DEFAULT_TAG;

    private static final String JDBC_DRIVER_CLASS_NAME = "org.postgresql.Driver";

    private static final String JDBC_URL_PREFIX = "jdbc:postgresql";

    private static final String TEST_QUERY_STRING = "SELECT 1";

    private static final int REST_API_PORT = 8080;

    private static final int DB_PORT = 26257;

    private static final String FIRST_VERSION_WITH_ENV_VARS_SUPPORT = "22.1.0";

    private String databaseName = "postgres";

    private String username = "root";

    private String password = "";

    private boolean isVersionGreaterThanOrEqualTo221;

    /**
     * @deprecated use {@link #CockroachContainer(DockerImageName)} instead
     */
    @Deprecated
    public CockroachContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public CockroachContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CockroachContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.isVersionGreaterThanOrEqualTo221 = isVersionGreaterThanOrEqualTo221(dockerImageName);

        WaitAllStrategy waitStrategy = new WaitAllStrategy();
        waitStrategy.withStrategy(
            Wait.forHttp("/health").forPort(REST_API_PORT).forStatusCode(200).withStartupTimeout(Duration.ofMinutes(1))
        );
        if (this.isVersionGreaterThanOrEqualTo221) {
            waitStrategy.withStrategy(Wait.forSuccessfulCommand("[ -f ./init_success ] || { exit 1; }"));
        }

        withExposedPorts(REST_API_PORT, DB_PORT);
        waitingFor(waitStrategy);
        withCommand("start-single-node --insecure");
    }

    @Override
    protected void configure() {
        withEnv("COCKROACH_USER", this.username);
        withEnv("COCKROACH_PASSWORD", this.password);
        if (this.password != null && !this.password.isEmpty()) {
            withCommand("start-single-node");
        }
        withEnv("COCKROACH_DATABASE", this.databaseName);
    }

    @Override
    public String getDriverClassName() {
        return JDBC_DRIVER_CLASS_NAME;
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return (
            JDBC_URL_PREFIX +
            "://" +
            getHost() +
            ":" +
            getMappedPort(DB_PORT) +
            "/" +
            databaseName +
            additionalUrlParams
        );
    }

    @Override
    public final String getDatabaseName() {
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
        return TEST_QUERY_STRING;
    }

    @Override
    public CockroachContainer withUsername(String username) {
        validateIfVersionSupportsUsernameOrPasswordOrDatabase("username");
        this.username = username;
        return this;
    }

    @Override
    public CockroachContainer withPassword(String password) {
        validateIfVersionSupportsUsernameOrPasswordOrDatabase("password");
        this.password = password;
        return this;
    }

    @Override
    public CockroachContainer withDatabaseName(final String databaseName) {
        validateIfVersionSupportsUsernameOrPasswordOrDatabase("databaseName");
        this.databaseName = databaseName;
        return this;
    }

    private boolean isVersionGreaterThanOrEqualTo221(DockerImageName dockerImageName) {
        ComparableVersion version = new ComparableVersion(dockerImageName.getVersionPart().replaceFirst("v", ""));
        return version.isGreaterThanOrEqualTo(FIRST_VERSION_WITH_ENV_VARS_SUPPORT);
    }

    private void validateIfVersionSupportsUsernameOrPasswordOrDatabase(String parameter) {
        if (!isVersionGreaterThanOrEqualTo221) {
            throw new UnsupportedOperationException(
                String.format("Setting a %s in not supported in the versions below 22.1.0", parameter)
            );
        }
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
