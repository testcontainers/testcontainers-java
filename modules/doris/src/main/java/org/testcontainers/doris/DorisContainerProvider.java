package org.testcontainers.doris;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Factory for Apache Doris containers.
 */
public class DorisContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String DEFAULT_TAG = "3.1.0";

    private static final String USER_PARAM = "user";

    private static final String PASSWORD_PARAM = "password";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(DorisContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new DorisContainer(DorisContainer.DOCKER_IMAGE_NAME.withTag(tag));
        } else {
            return newInstance();
        }
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        DorisContainer container;
        if (connectionUrl.getImageTag().isPresent()) {
            container = new DorisContainer(DorisContainer.DOCKER_IMAGE_NAME.withTag(connectionUrl.getImageTag().get()));
        } else {
            container = (DorisContainer) newInstance();
        }

        connectionUrl.getDatabaseName().ifPresent(container::withDatabaseName);
        if (connectionUrl.getQueryParameters().containsKey(USER_PARAM)) {
            container.withUsername(connectionUrl.getQueryParameters().get(USER_PARAM));
        }
        if (connectionUrl.getQueryParameters().containsKey(PASSWORD_PARAM)) {
            container.withPassword(connectionUrl.getQueryParameters().get(PASSWORD_PARAM));
        }

        return container.withReuse(connectionUrl.isReusable());
    }
}
