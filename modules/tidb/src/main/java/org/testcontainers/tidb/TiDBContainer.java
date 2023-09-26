package org.testcontainers.tidb;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;

/**
 * Testcontainers implementation for TiDB.
 * <p>
 * Supported image: {@code pingcap/tidb}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 4000</li>
 *     <li>HTTP: 10080</li>
 * </ul>
 */
public class TiDBContainer extends JdbcDatabaseContainer<TiDBContainer> {

    static final String NAME = "tidb";

    static final String DOCKER_IMAGE_NAME = "pingcap/tidb";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(DOCKER_IMAGE_NAME);

    private static final Integer TIDB_PORT = 4000;

    private static final int REST_API_PORT = 10080;

    private String databaseName = "test";

    private String username = "root";

    private String password = "";

    public TiDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public TiDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(TIDB_PORT, REST_API_PORT);

        waitingFor(
            new HttpWaitStrategy()
                .forPath("/status")
                .forPort(REST_API_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(1))
        );
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
    public String getDriverClassName() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return "com.mysql.cj.jdbc.Driver";
        } catch (ClassNotFoundException e) {
            return "com.mysql.jdbc.Driver";
        }
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return "jdbc:mysql://" + getHost() + ":" + getMappedPort(TIDB_PORT) + "/" + databaseName + additionalUrlParams;
    }

    @Override
    protected String constructUrlForConnection(String queryString) {
        String url = super.constructUrlForConnection(queryString);

        if (!url.contains("useSSL=")) {
            String separator = url.contains("?") ? "&" : "?";
            url = url + separator + "useSSL=false";
        }

        if (!url.contains("allowPublicKeyRetrieval=")) {
            url = url + "&allowPublicKeyRetrieval=true";
        }

        return url;
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
    public TiDBContainer withDatabaseName(final String databaseName) {
        throw new UnsupportedOperationException("The TiDB docker image does not currently support this");
    }

    @Override
    public TiDBContainer withUsername(final String username) {
        throw new UnsupportedOperationException("The TiDB docker image does not currently support this");
    }

    @Override
    public TiDBContainer withPassword(final String password) {
        throw new UnsupportedOperationException("The TiDB docker image does not currently support this");
    }
}
