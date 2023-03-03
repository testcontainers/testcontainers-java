package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * YugabyteDB YSQL (Structured Query Language) JDBC container provider
 */
public class YugabyteDBYSQLContainerProvider extends JdbcDatabaseContainerProvider {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("yugabytedb/yugabyte");

    private static final String DEFAULT_TAG = "2.14.4.0-b26";

    private static final String NAME = "yugabyte";

    private static final String USER_PARAM = "user";

    private static final String PASSWORD_PARAM = "password";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new YugabyteDBYSQLContainer(DEFAULT_IMAGE_NAME.withTag(tag));
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
