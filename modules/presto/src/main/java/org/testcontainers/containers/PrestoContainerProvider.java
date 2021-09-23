package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Presto containers.
 */
public class PrestoContainerProvider extends JdbcDatabaseContainerProvider<PrestoContainer> {

    public static final String USER_PARAM = "user";
    public static final String PASSWORD_PARAM = "password";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(PrestoContainer.NAME);
    }

    @Override
    public PrestoContainer newInstance() {
        return newInstance(PrestoContainer.DEFAULT_TAG);
    }

    @Override
    public PrestoContainer newInstance(String tag) {
        return new PrestoContainer(DockerImageName.parse(PrestoContainer.IMAGE).withTag(tag));
    }

    @Override
    public PrestoContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }

}
