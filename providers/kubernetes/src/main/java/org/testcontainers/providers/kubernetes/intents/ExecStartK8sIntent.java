package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import org.testcontainers.controller.intents.ExecStartIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionErrorListener;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionLogCallbackAdapter;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecution;
import org.testcontainers.providers.kubernetes.execution.KubernetesExecutionListener;
import org.testcontainers.providers.kubernetes.execution.NullInputStream;

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
    public <T extends ResultCallback<Frame>> T perform(T resultCallback) {
        Pod pod = ctx.findPodForContainerId(command.getContainerId());

        KubernetesExecutionListener listener = new KubernetesExecutionListener();
        KubernetesExecutionErrorListener errorListener = listener.getErrorListener();

        PodResource<Pod> podPodResource = ctx.getClient()
            .pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName());


        ExecWatch execWatch;
        NullInputStream nullInputStream = new NullInputStream();
        if(command.isAttachStderr() && command.isAttachStdout()) {
            KubernetesExecutionLogCallbackAdapter<T> err = new KubernetesExecutionLogCallbackAdapter<>(StreamType.STDERR, resultCallback);
            KubernetesExecutionLogCallbackAdapter<T> out = new KubernetesExecutionLogCallbackAdapter<>(StreamType.STDOUT, resultCallback);
            execWatch = podPodResource
                .readingInput(nullInputStream)
                .writingOutput(out)
                .writingError(err)
                .writingErrorChannel(errorListener)
                .usingListener(listener.withLogAdapters(out, err))
                .exec(command.getCommand());
        } else if(command.isAttachStdout()) {
            KubernetesExecutionLogCallbackAdapter<T> out = new KubernetesExecutionLogCallbackAdapter<>(StreamType.STDOUT, resultCallback);
            execWatch = podPodResource
                .readingInput(nullInputStream)
                .writingOutput(out)
                .writingErrorChannel(errorListener)
                .usingListener(listener.withLogAdapters(out))
                .exec(command.getCommand());
        } else if(command.isAttachStderr()) {
            KubernetesExecutionLogCallbackAdapter<T> err = new KubernetesExecutionLogCallbackAdapter<>(StreamType.STDERR, resultCallback);
            execWatch = podPodResource
                .readingInput(nullInputStream)
                .writingError(err)
                .writingErrorChannel(errorListener)
                .usingListener(listener.withLogAdapters(err))
                .exec(command.getCommand());
        } else {
            execWatch = podPodResource
                .writingErrorChannel(System.err)
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
