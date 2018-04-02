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
        return new OracleContainer(OracleContainer.IMAGE + ":" + tag);
    }
}
