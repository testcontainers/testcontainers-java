package org.testcontainers.containers;

import java.util.Optional;

/**
 * Factory for MariaDB org.testcontainers.containers.
 */
public class MariaDBContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MariaDBContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(Optional<String> tag) {
        return new MariaDBContainer(MariaDBContainer.IMAGE + ":" + tag.orElse(MariaDBContainer.DEFAULT_TAG));
    }
}
