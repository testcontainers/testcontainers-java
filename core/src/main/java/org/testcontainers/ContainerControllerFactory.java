package org.testcontainers;

import lombok.SneakyThrows;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ContainerProvider;
import org.testcontainers.controller.ContainerProviderInitParams;
import org.testcontainers.controller.NoSuchProviderException;
import org.testcontainers.utility.TestcontainersConfiguration;

public class ContainerControllerFactory { // TODO: Rename to ContainerProviderFactory

    private static ContainerProvider instance;

    private static final ContainerProviderElector elector = new ContainerProviderElector(
        TestcontainersConfiguration.getInstance()
    );

    /**
     * @deprecated Use {@link ContainerProvider#lazyController()}
     * @return
     */
    @Deprecated
    public static ContainerController lazyController() {
        return instance().lazyController();
    }

    @SneakyThrows({NoSuchProviderException.class})
    public synchronized static ContainerProvider instance() {
        if (instance == null) {
            instance = elector.elect().init(
                new ContainerProviderInitParams(
                    TestcontainersConfiguration.getInstance()
                )
            );
        }
        return instance;
    }

}
