package org.testcontainers.junit.jupiter;

import org.testcontainers.DockerClientFactory;

class DockerAvailableDetector {

    public boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }
}
