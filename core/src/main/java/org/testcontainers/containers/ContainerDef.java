package org.testcontainers.containers;

import org.testcontainers.UnstableAPI;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

@UnstableAPI
class ContainerDef extends BaseContainerDef<StartedContainer> {

    public ContainerDef(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public ContainerDef(DockerImageName image) {
        super(new RemoteDockerImage(image));
    }

    public ContainerDef(RemoteDockerImage image) {
        super(image);
    }

    @Override
    protected StartedContainer toStarted(ContainerState container) {
        return new BaseContainerDef.Started(container);
    }
}
