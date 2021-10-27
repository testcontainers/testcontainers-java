package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * @deprecated Use {@code TrinoContainer} instead.
 */
@Deprecated
public class PrestoContainer<SELF extends PrestoContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "presto";
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/trinodb/presto");
    public static final String IMAGE = "ghcr.io/trinodb/presto";
    public static final String DEFAULT_TAG = "344";

    public static final Integer PRESTO_PORT = 8080;

    private String username = "test";
    private String catalog = null;

    /**
     * @deprecated use {@link PrestoContainer(DockerImageName)} instead
     */
    @Deprecated
    public PrestoContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public PrestoContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public PrestoContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy = new LogMessageWaitStrategy()
            .withRegEx(".*======== SERVER STARTED ========.*")
            .withStartupTimeout(Duration.of(60, SECONDS));

        addExposedPort(PRESTO_PORT);
    }

    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(PRESTO_PORT));
    }

    @Override
    public String getDriverClassName() {
        return "io.prestosql.jdbc.PrestoDriver";
    }

    @Override
    public String getJdbcUrl() {
        return format("jdbc:presto://%s:%s/%s", getHost(), getMappedPort(PRESTO_PORT), nullToEmpty(catalog));
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
    public SELF withUsername(final String username) {
        this.username = username;
        return self();
    }

    /**
     * @deprecated This operation is not supported.
     */
    @Override
    @Deprecated
    public SELF withPassword(final String password) {
        // ignored, Presto does not support password authentication without TLS
        // TODO: make JDBCDriverTest not pass a password unconditionally and remove this method
        return self();
    }

    @Override
    public SELF withDatabaseName(String dbName) {
        this.catalog = dbName;
        return self();
    }

    public Connection createConnection() throws SQLException, NoDriverFoundException {
        return createConnection("");
    }
}
