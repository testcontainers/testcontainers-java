package org.testcontainers.containers;

/**
 * Factory for Oracle containers.
 */
public class OracleContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OracleContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {

        if (!tag.equalsIgnoreCase("latest")) {
            throw new UnsupportedOperationException("Oracle database tag should be set in the configured image name");
        }

        return new OracleContainer();
    }
}
