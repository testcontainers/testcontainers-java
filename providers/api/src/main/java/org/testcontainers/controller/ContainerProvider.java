package org.testcontainers.controller;

public interface ContainerProvider {

    ContainerProvider init(ContainerProviderInitParams params);

    ContainerController lazyController();
    ContainerController controller();

    boolean supportsExecution();

    boolean isFileMountingSupported();

    String getIdentifier();

    boolean isAvailable();
}
