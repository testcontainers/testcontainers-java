package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for PgVector containers.
 *
 * @see <a href="https://github.com/pgvector/pgvector">https://github.com/pgvector/pgvector</a>
 */
public class PgVectorContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "pgvector";

    private static final String DEFAULT_TAG = "pg16";

    private static final DockerImageName DEFAULT_IMAGE = DockerImageName
        .parse("pgvector/pgvector")
        .asCompatibleSubstituteFor("postgres");

    public static final String USER_PARAM = "user";

    public static final String PASSWORD_PARAM = "password";

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
        return new PostgreSQLContainer(DEFAULT_IMAGE.withTag(tag));
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
