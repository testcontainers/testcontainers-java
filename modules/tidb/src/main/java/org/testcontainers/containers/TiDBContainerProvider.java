package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for TiDB containers.
 */
public class TiDBContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String USER_PARAM = "root";

    private static final String PASSWORD_PARAM = "";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TiDBContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(TiDBContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new TiDBContainer(DockerImageName.parse(TiDBContainer.IMAGE).withTag(tag));
        } else {
            return newInstance();
        }
    }
}
