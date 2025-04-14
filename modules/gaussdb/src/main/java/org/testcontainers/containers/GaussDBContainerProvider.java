package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for GaussDB containers.
 */
public class GaussDBContainerProvider extends JdbcDatabaseContainerProvider {

    public static final String USER_PARAM = "user";

    public static final String PASSWORD_PARAM = "password";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(GaussDBContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(GaussDBContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new GaussDBContainer(DockerImageName.parse(GaussDBContainer.IMAGE).withTag(tag));
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
