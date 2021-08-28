package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.testcontainers.controller.intents.ExecStartIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionLogAdapter;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecution;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionListener;
import org.testcontainers.providers.kubernetes.execution.NullInputStream;

import java.io.InputStream;
import java.io.OutputStream;

public class ExecStartK8sIntent implements ExecStartIntent {
    private final KubernetesContext ctx;
    private final String commandId;
    private final ExecCreateK8sIntent command;

    public ExecStartK8sIntent(
        KubernetesContext ctx,
        String commandId,
        ExecCreateK8sIntent command
    ) {
        this.ctx = ctx;
        this.commandId = commandId;
        this.command = command;
    }

    @Override
    public <T extends ResultCallback<Frame>> T exec(T resultCallback) {
        Pod pod = ctx.findPodForContainerId(command.getContainerId());

        KubernetesExecutionListener listener = new KubernetesExecutionListener();

        PodResource<Pod> podPodResource = ctx.getClient()
            .pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName());


        ExecWatch execWatch;
        NullInputStream nullInputStream = new NullInputStream();
        if(command.isAttachStderr() && command.isAttachStdout()) {
            KubernetesExecutionLogAdapter<T> err = new KubernetesExecutionLogAdapter<>(StreamType.STDERR, resultCallback);
            KubernetesExecutionLogAdapter<T> out = new KubernetesExecutionLogAdapter<>(StreamType.STDOUT, resultCallback);
            execWatch = podPodResource
                .readingInput(nullInputStream)
                .writingOutput(out)
                .writingError(err)
                .usingListener(listener.withLogAdapters(out, err))
                .exec(command.getCommand());
        } else if(command.isAttachStdout()) {
            KubernetesExecutionLogAdapter<T> out = new KubernetesExecutionLogAdapter<>(StreamType.STDOUT, resultCallback);
            execWatch = podPodResource
                .readingInput(nullInputStream)
                .writingOutput(out)
                .usingListener(listener.withLogAdapters(out))
                .exec(command.getCommand());
        } else if(command.isAttachStderr()) {
            KubernetesExecutionLogAdapter<T> err = new KubernetesExecutionLogAdapter<>(StreamType.STDERR, resultCallback);
            execWatch = podPodResource
                .readingInput(nullInputStream)
                .writingOutput(err)
                .usingListener(listener.withLogAdapters(err))
                .exec(command.getCommand());
        } else {
            execWatch = podPodResource
                .usingListener(listener)
                .exec(command.getCommand());
        }

        ctx.registerCommandWatch(
            commandId,
            new KubernetesExecution(
                listener,
                execWatch
            )
        );
        return resultCallback;
    }
}
