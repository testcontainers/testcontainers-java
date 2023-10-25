package org.testcontainers.cratedb;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;

/**
 * Testcontainers implementation for CrateDB.
 * <p>
 * Supported image: {@code crate}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 5432</li>
 *     <li>Console: 4200</li>
 * </ul>
 */
public class CrateDBContainer extends JdbcDatabaseContainer<CrateDBContainer> {

    static final String NAME = "cratedb";

    static final String IMAGE = "crate";

    static final String DEFAULT_TAG = "5.3.1";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("crate");

    static final Integer CRATEDB_PG_PORT = 5432;

    static final Integer CRATEDB_HTTP_PORT = 4200;

    private String databaseName = "crate";

    private String username = "crate";

    private String password = "crate";

    public CrateDBContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CrateDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withCommand("crate -C discovery.type=single-node");

        setWaitStrategy(Wait.forHttp("/").forPort(CRATEDB_HTTP_PORT).forStatusCode(200));

        addExposedPort(CRATEDB_PG_PORT);
        addExposedPort(CRATEDB_HTTP_PORT);
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
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return (
            "jdbc:postgresql://" +
            getHost() +
            ":" +
            getMappedPort(CRATEDB_PG_PORT) +
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
    public CrateDBContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public CrateDBContainer withUsername(final String username) {
        this.username = username;
        return self();
    }

    @Override
    public CrateDBContainer withPassword(final String password) {
        this.password = password;
        return self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
