package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for CrateDB containers using its own legacy JDBC driver.
 */
public class CrateDBLegacyDriverContainerProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(CrateDBLegacyDriverContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance(String tag) {
        return new CrateDBLegacyDriverContainer<>(DockerImageName.parse(CrateDBContainer.IMAGE).withTag(tag));
    }
}
