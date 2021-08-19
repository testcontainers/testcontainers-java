package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.OracleContainer.DEFAULT_DATABASE_NAME;

/**
 * Factory for Oracle containers.
 */
public class OracleContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OracleContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(OracleContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new OracleContainer(DockerImageName.parse(OracleContainer.IMAGE).withTag(tag));
        }
        return newInstance();
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        Objects.requireNonNull(connectionUrl, "Connection URL cannot be null");

        final String databaseName = connectionUrl.getDatabaseName().orElse(DEFAULT_DATABASE_NAME);

        final OracleContainer instance = new OracleContainer()
            .withReuse(connectionUrl.isReusable())
            .withDatabaseName(databaseName);

        Matcher urlMatcher = ConnectionUrl.Patterns.ORACLE_URL_MATCHING_PATTERN.matcher(connectionUrl.getUrl());
        if (urlMatcher.matches()) {
            Optional.ofNullable(urlMatcher.group("username")).ifPresent(instance::withUsername);
            Optional.ofNullable(urlMatcher.group("password")).ifPresent(instance::withPassword);

            Matcher databaseHostMatcher = ConnectionUrl.Patterns.DB_INSTANCE_MATCHING_PATTERN.matcher(connectionUrl.getDbHostString());
            if (databaseHostMatcher.matches()) {
                Optional.ofNullable(databaseHostMatcher.group("sidOrServiceName")).ifPresent(instance::withSidOrServiceName);
            }
        }

        return instance;
    }
}
