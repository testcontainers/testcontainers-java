package org.testcontainers.containers;

/**
 * Created by rnorth on 21/07/2015.
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
