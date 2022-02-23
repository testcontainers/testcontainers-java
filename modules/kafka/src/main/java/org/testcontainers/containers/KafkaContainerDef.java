package org.testcontainers.containers;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import org.testcontainers.images.RemoteDockerImage;

class KafkaContainerDef extends BaseContainerDef {

    @Getter(AccessLevel.MODULE)
    protected String externalZookeeperConnect = null;

    KafkaContainerDef(@NonNull RemoteDockerImage image) {
        super(image);
    }

    public void withEmbeddedZookeeper() {
        externalZookeeperConnect = null;
    }

    public void withExternalZookeeper(String connectString) {
        externalZookeeperConnect = connectString;
    }
}
