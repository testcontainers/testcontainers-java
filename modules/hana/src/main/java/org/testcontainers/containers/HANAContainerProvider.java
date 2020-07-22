package org.testcontainers.containers;

/**
 * Factory for HANA containers.
 */
public class HANAContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String USER_PARAM = "SYSTEM";
    private static final String PASSWORD_PARAM = "HXEHana1";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(HANAContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(HANAContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new HANAContainer(HANAContainer.IMAGE + ":" + tag);
        } else {
            return newInstance();
        }
    }
}
