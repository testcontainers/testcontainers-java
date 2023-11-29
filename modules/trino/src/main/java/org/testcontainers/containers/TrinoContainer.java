package org.testcontainers.containers;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Testcontainers implementation for TrinoDB.
 * <p>
 * Supported image: {@code trinodb/trino}
 * <p>
 * Exposed ports: 8080
 */
public class TrinoContainer extends JdbcDatabaseContainer<TrinoContainer> {

    static final String NAME = "trino";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("trinodb/trino");

    static final String IMAGE = "trinodb/trino";

    @VisibleForTesting
    static final String DEFAULT_TAG = "352";

    private static final int TRINO_PORT = 8080;

    private String username = "test";

    private String catalog = null;

    public TrinoContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public TrinoContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPort(TRINO_PORT);
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
        return "io.trino.jdbc.TrinoDriver";
    }

    @Override
    public String getJdbcUrl() {
        return String.format(
            "jdbc:trino://%s:%s/%s",
            getHost(),
            getMappedPort(TRINO_PORT),
            Strings.nullToEmpty(catalog)
        );
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getDatabaseName() {
        return catalog;
    }

    @Override
    public String getTestQueryString() {
        return "SELECT count(*) FROM tpch.tiny.nation";
    }

    @Override
    public TrinoContainer withUsername(final String username) {
        this.username = username;
        return this;
    }

    @Override
    public TrinoContainer withDatabaseName(String dbName) {
        this.catalog = dbName;
        return this;
    }

    public Connection createConnection() throws SQLException, NoDriverFoundException {
        return createConnection("");
    }
}
