package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import org.testcontainers.controller.intents.RemoveContainerIntent;

import java.util.Optional;

public class RemoveContainerK8sIntent implements RemoveContainerIntent {

    private final KubernetesContext ctx;
    private final ReplicaSet replicaSet;

    private boolean withForce = false;
    private boolean withRemoveVolumes = false;

    public RemoveContainerK8sIntent(KubernetesContext ctx, ReplicaSet replicaSet) {
        this.ctx = ctx;
        this.replicaSet = replicaSet;
    }

    @Override
    public RemoveContainerIntent withRemoveVolumes(boolean removeVolumes) {
        this.withRemoveVolumes = removeVolumes;
        return this;
    }

    @Override
    public RemoveContainerIntent withForce(boolean force) {
        this.withForce = force;
        return this;
    }

    @Override
    public void perform() {
        Optional<Service> service = ctx.findServiceForReplicaSet(replicaSet);
        service.ifPresent(value -> ctx.getClient()
            .services()
            .delete(value));

        RollableScalableResource<ReplicaSet> deleteSelector = ctx.getClient()
            .apps()
            .replicaSets()
            .inNamespace(replicaSet.getMetadata().getNamespace())
            .withName(replicaSet.getMetadata().getName());

        if(withForce) {
            deleteSelector.withGracePeriod(0).delete();
        } else {
            deleteSelector.delete();
        }

    }
}
