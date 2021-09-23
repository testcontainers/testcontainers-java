package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for PostgreSQL containers.
 */
public class PostgreSQLContainerProvider extends JdbcDatabaseContainerProvider<PostgreSQLContainer> {

    public static final String USER_PARAM = "user";
    public static final String PASSWORD_PARAM = "password";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(PostgreSQLContainer.NAME);
    }

    @Override
    public PostgreSQLContainer newInstance() {
        return newInstance(PostgreSQLContainer.DEFAULT_TAG);
    }

    @Override
    public PostgreSQLContainer newInstance(String tag) {
        return new PostgreSQLContainer(DockerImageName.parse(PostgreSQLContainer.IMAGE).withTag(tag));
    }

    @Override
    public PostgreSQLContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }

}
