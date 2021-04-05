package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Strings.nullToEmpty;
import static java.lang.String.format;

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

    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return new HashSet<>(getMappedPort(TRINO_PORT));
    }

    @Override
    public String getDriverClassName() {
        return "io.trino.jdbc.TrinoDriver";
    }

    @Override
    public String getJdbcUrl() {
        return format("jdbc:trino://%s:%s/%s", getHost(), getMappedPort(TRINO_PORT), nullToEmpty(catalog));
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
