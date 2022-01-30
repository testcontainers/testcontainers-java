package org.testcontainers.containers;

import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;

public class ClickHouseContainer extends JdbcDatabaseContainer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClickHouseContainer.class);

    public static final String NAME = "clickhouse";

    private static final DockerImageName DEFAULT_IMAGE_NAME;

    @Deprecated
    public static final String IMAGE;

    @Deprecated
    public static final String DEFAULT_TAG;

    public static final Integer HTTP_PORT = 8123;
    public static final Integer NATIVE_PORT = 9000;

    private static final String DRIVER_CLASS_NAME;
    private static final String DEPRECATED_DRIVER_CLASS_NAME = "ru.yandex.clickhouse.ClickHouseDriver";
    private static final String LATEST_DRIVER_CLASS_NAME = "com.clickhouse.jdbc.ClickHouseDriver";
    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";
    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";
    private String username = "default";
    private String password = "";

    static {
        // TODO: Future versions of Testcontainers will not support the old driver after July 2022 (https://github.com/testcontainers/testcontainers-java/issues/4924)
        boolean temporarilyUseDeprecatedDriver = Boolean.getBoolean("clickhouse-temporarily-use-deprecated-driver");
        if (temporarilyUseDeprecatedDriver) {
            DEFAULT_IMAGE_NAME = DockerImageName.parse("yandex/clickhouse-server");
            IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();
            DEFAULT_TAG = "18.10.3";
            DRIVER_CLASS_NAME = DEPRECATED_DRIVER_CLASS_NAME;

            LOGGER.warn("Future versions of Testcontainers will not support the old ClickHouse driver[{}] after July 2022.", DEPRECATED_DRIVER_CLASS_NAME);
            LOGGER.warn("It is recommended that you to use ClickHouse version 20.7 or above.");
            LOGGER.warn("You may temporarily continue running on old ClickHouse driver[{}] by adding the following", DEPRECATED_DRIVER_CLASS_NAME);
            LOGGER.warn("JVM config option:");
            LOGGER.warn("    -Dclickhouse-temporarily-use-deprecated-driver=true");
        } else {
            DEFAULT_IMAGE_NAME = DockerImageName.parse("clickhouse/clickhouse-server");
            IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();
            DEFAULT_TAG = "21.3";
            DRIVER_CLASS_NAME = LATEST_DRIVER_CLASS_NAME;
        }
    }

    /**
     * @deprecated use {@link ClickHouseContainer(DockerImageName)} instead
     */
    @Deprecated
    public ClickHouseContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public ClickHouseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ClickHouseContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(HTTP_PORT, NATIVE_PORT);
        waitingFor(
            new HttpWaitStrategy()
                .forStatusCode(200)
                .forResponsePredicate(responseBody -> "Ok.".equals(responseBody))
                .withStartupTimeout(Duration.ofMinutes(1))
        );
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Sets.newHashSet(HTTP_PORT);
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
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
    public ClickHouseContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The ClickHouse does not support this");
    }
}
