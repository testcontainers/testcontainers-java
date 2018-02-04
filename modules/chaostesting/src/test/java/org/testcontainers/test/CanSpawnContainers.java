package org.testcontainers.test;

/**
 * Created by novy on 01.01.17.
 */
interface CanSpawnContainers {

    default Container startedContainer() {
        final Container aContainer = new Container();
        aContainer.start();
        return aContainer;
    }

    default Container startedContainerWithName(String containerName) {
        return startedContainer().renameTo(containerName);
    }

    default Pinger startedPinger() {
        final Pinger pinger = new Pinger();
        pinger.start();
        return pinger;
    }
}
