package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.intents.LogContainerIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionLogCallbackAdapter;

import java.io.OutputStreamWriter;

@Slf4j
public class LogContainerK8sIntent implements LogContainerIntent {
    private final KubernetesContext ctx;
    private final String containerId;
    private boolean followStream;
    private boolean withStdOut;
    private boolean withStdErr;


    public LogContainerK8sIntent(KubernetesContext ctx, String containerId) {
        this.ctx = ctx;
        this.containerId = containerId;
    }

    @Override
    public LogContainerIntent withSince(int i) {
        log.warn("Kubernetes provider does not support since option (ignoring)"); // TODO: Double check
        return this;
    }

    @Override
    public LogContainerIntent withFollowStream(boolean followStream) {
        this.followStream = followStream;
        return this;
    }

    @Override
    public LogContainerIntent withStdOut(boolean withStdOut) {
        this.withStdOut = withStdOut;
        return this;
    }

    @Override
    public LogContainerIntent withStdErr(boolean withStdErr) {
        this.withStdErr = withStdErr;
        return this;
    }

    @Override
    @SneakyThrows
    public <T extends ResultCallback<Frame>> T perform(T resultCallback) {
        Pod pod = ctx.findPodForContainerId(containerId);
        PodResource<Pod> podResource = ctx.getClient().pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName());

        if(followStream) {
            KubernetesExecutionLogCallbackAdapter<T> logAdapter = new KubernetesExecutionLogCallbackAdapter<T>(StreamType.STDOUT, resultCallback);
            podResource.watchLog(logAdapter);

        } else {
            String log = podResource.getLog();
            try(KubernetesExecutionLogCallbackAdapter<T> logAdapter = new KubernetesExecutionLogCallbackAdapter<>(StreamType.STDOUT, resultCallback)) {
                try(OutputStreamWriter writer = new OutputStreamWriter(logAdapter)){
                    writer.write(log);
                }
            }
        }

        return resultCallback;
    }
}
