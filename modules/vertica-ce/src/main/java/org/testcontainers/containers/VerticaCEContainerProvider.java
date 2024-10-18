package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Vertica-CE containers.
 */
@SuppressWarnings("rawtypes")
public class VerticaCEContainerProvider extends JdbcDatabaseContainerProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(VerticaCEContainer.NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(VerticaCEContainer.DEFAULT_TAG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new VerticaCEContainer(DockerImageName.parse(VerticaCEContainer.IMAGE).withTag(tag));
    }
}
