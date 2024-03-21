package org.testcontainers.containers;

class ContainerDef extends BaseContainerDef {

    @Override
    protected StartedContainer toStarted(ContainerState containerState) {
        return new Started(containerState);
    }
}
