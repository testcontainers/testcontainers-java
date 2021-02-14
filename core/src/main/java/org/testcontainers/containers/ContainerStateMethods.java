package org.testcontainers.containers;

import com.github.dockerjava.api.exception.DockerException;

class ContainerStateMethods {
    /**
     * @return does the container exist?
     */
    static boolean exists(ContainerState state) {
        if (state.getContainerId() == null) {
            return false;
        }

        try {
            String status = state.getCurrentContainerInfo().getState().getStatus();
            return status != null && !status.isEmpty();
        }
        catch (DockerException e) {
            return false;
        }
    }
}
