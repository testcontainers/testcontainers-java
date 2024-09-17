package org.testcontainers.databend;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.HashSet;
import java.util.Set;

/**
 * Testcontainers implementation for Databend.
 * <p>
 * Supported image: {@code datafuselabs/databend}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 8000</li>
 * </ul>
 */
public class DatabendContainer extends JdbcDatabaseContainer<DatabendContainer> {

    static final String NAME = "databend";

    static final DockerImageName DOCKER_IMAGE_NAME = DockerImageName.parse("datafuselabs/databend");

    private static final Integer HTTP_PORT = 8000;

    private static final String DRIVER_CLASS_NAME = "com.databend.jdbc.DatabendDriver";

    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";

    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";

    private String username = "databend";

    private String password = "databend";

    public DatabendContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public DatabendContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DOCKER_IMAGE_NAME);

        addExposedPorts(HTTP_PORT);
        waitingFor(Wait.forHttp("/").forResponsePredicate(response -> response.equals("Ok.")));
    }

    @Override
    protected void configure() {
        withEnv("QUERY_DEFAULT_USER", this.username);
        withEnv("QUERY_DEFAULT_PASSWORD", this.password);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return new HashSet<>(getMappedPort(HTTP_PORT));
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
    }

    @Override
    public String getJdbcUrl() {
        return (
            JDBC_URL_PREFIX +
            getHost() +
            ":" +
            getMappedPort(HTTP_PORT) +
            "/" +
            this.databaseName +
            constructUrlParameters("?", "&")
        );
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getDatabaseName() {
        return this.databaseName;
    }

    @Override
    public String getTestQueryString() {
        return TEST_QUERY;
    }

    @Override
    public DatabendContainer withUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public DatabendContainer withPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public DatabendContainer withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }
}
