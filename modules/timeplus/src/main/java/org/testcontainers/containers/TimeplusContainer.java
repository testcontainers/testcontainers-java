package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Testcontainers implementation for timeplus.
 */
public class TimeplusContainer extends JdbcDatabaseContainer<TimeplusContainer> {

    public static final String NAME = "timeplus";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("timeplus/timeplusd:2.2.10");

    private static final DockerImageName TIMEPLUS_IMAGE_NAME = DockerImageName.parse("timeplus/timeplusd:2.3.3");

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    @Deprecated
    public static final String DEFAULT_TAG = "latest";

    public static final Integer HTTP_PORT = 3128;

    public static final Integer NATIVE_PORT = 8463;

    private static final String LEGACY_DRIVER_CLASS_NAME = "ru.yandex.clickhouse.ClickHouseDriver";

    private static final String DRIVER_CLASS_NAME = "com.timeplus.jdbc.TimeplusDriver";

    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";

    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";

    private String username = "proton";

    private String password = "proton@t+";

    private boolean supportsNewDriver;

    /**
     * @deprecated use {@link #TimeplusContainer(DockerImageName)} instead
     */
    @Deprecated
    public TimeplusContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public TimeplusContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public TimeplusContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, TIMEPLUS_IMAGE_NAME);
        supportsNewDriver = isNewDriverSupported(dockerImageName);

        addExposedPorts(HTTP_PORT, NATIVE_PORT);
        this.waitStrategy =
            new HttpWaitStrategy()
                .forStatusCode(200)
                .forResponsePredicate("Ok."::equals)
                .withStartupTimeout(Duration.ofMinutes(1));
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return new HashSet<>(getMappedPort(HTTP_PORT));
    }

    @Override
    public String getDriverClassName() {
        try {
            if (supportsNewDriver) {
                Class.forName(DRIVER_CLASS_NAME);
                return DRIVER_CLASS_NAME;
            } else {
                return LEGACY_DRIVER_CLASS_NAME;
            }
        } catch (ClassNotFoundException e) {
            return LEGACY_DRIVER_CLASS_NAME;
        }
    }

    private static boolean isNewDriverSupported(DockerImageName dockerImageName) {
        // New driver supports versions 20.7+. Check the version part of the tag
        return new ComparableVersion(dockerImageName.getVersionPart()).isGreaterThanOrEqualTo("20.7");
    }

    @Override
    public String getJdbcUrl() {
        return JDBC_URL_PREFIX + getHost() + ":" + getMappedPort(HTTP_PORT) + "/" + databaseName;
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
        return TEST_QUERY;
    }

    @Override
    public TimeplusContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The Timeplus does not support this");
    }
}
