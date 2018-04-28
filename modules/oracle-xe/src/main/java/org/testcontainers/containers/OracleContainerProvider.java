package org.testcontainers.containers;

import java.util.Optional;

/**
 * Factory for Oracle containers.
 */
public class OracleContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OracleContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(Optional<String> tag) {

        if (!tag.isPresent()) {
            throw new UnsupportedOperationException("Oracle database tag should be set in the configured image name");
        }

        return new OracleContainer();
    }
}
