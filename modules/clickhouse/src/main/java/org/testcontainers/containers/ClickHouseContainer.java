package org.testcontainers.containers;

import com.google.common.collect.Sets;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;

public class ClickHouseContainer extends JdbcDatabaseContainer {
    public static final String NAME = "clickhouse";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("yandex/clickhouse-server");

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    @Deprecated
    public static final String DEFAULT_TAG = "18.10.3";

    public static final Integer HTTP_PORT = 8123;
    public static final Integer NATIVE_PORT = 9000;

    private static final String DRIVER_CLASS_NAME = "ru.yandex.clickhouse.ClickHouseDriver";
    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";
    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";
    private String username = "default";
    private String password = "";

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
