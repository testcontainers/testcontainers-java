package org.testcontainers.containers;

import org.testcontainers.images.RemoteDockerImage;

class ContainerDef extends BaseContainerDef {
    ContainerDef(RemoteDockerImage image) {
        super(image);
    }
}
