package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Original Testcontainers implementation for CosmosDB Emulator.
 *
 * @deprecated Please use {@link org.testcontainers.azure.CosmosDBEmulatorContainer}.
 */
@Deprecated
public class CosmosDBEmulatorContainer extends org.testcontainers.azure.CosmosDBEmulatorContainer {

    /**
     * @param dockerImageName specified docker image name to run
     * @deprecated Please use {@link org.testcontainers.azure.CosmosDBEmulatorContainer}.
     */
    @Deprecated
    public CosmosDBEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        logger()
            .warn(
                "The CosmosDBEmulatorContainer moved to the org.testcontainers.azure package. This old class will be removed in a future version."
            );
    }
}
