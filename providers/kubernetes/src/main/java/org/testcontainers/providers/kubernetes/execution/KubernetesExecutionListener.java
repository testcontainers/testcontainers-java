package org.testcontainers.providers.kubernetes.execution;

import io.fabric8.kubernetes.client.dsl.ExecListener;
import lombok.SneakyThrows;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;

public class KubernetesExecutionListener implements ExecListener {

    private final CountDownLatch execLatch = new CountDownLatch(1);

    private final List<KubernetesExecutionLogCallbackAdapter<?>> logAdapters = new ArrayList<>();

    private final KubernetesExecutionErrorListener errorListener = new KubernetesExecutionErrorListener();


    public KubernetesExecutionListener withLogAdapters(KubernetesExecutionLogCallbackAdapter<?>... adapters) {
        logAdapters.addAll(Arrays.asList(adapters));
        return this;
    }

    @SneakyThrows
    private void closeAdapters() {
        for (KubernetesExecutionLogCallbackAdapter<?> adapter : logAdapters) {
            adapter.close();
        }
    }

    @Override
    public void onOpen(Response response) {
        // We don't care about the start event here
    }

    @Override
    public void onFailure(Throwable t, Response response) {
        execLatch.countDown();
        closeAdapters();
    }

    @Override
    public void onClose(int code, String reason) {

        execLatch.countDown();
        closeAdapters();
    }

    @SneakyThrows
    public void waitForCompletion() {
        execLatch.await();
    }

    @SneakyThrows
    public Integer getExitCode() {
        return Optional.ofNullable(errorListener.getExitInformation())
            .map(ProcessExitInformation::getExitCode)
            .orElse(null);
    }

    @SneakyThrows
    public String getReason() {
        return Optional.ofNullable(errorListener.getExitInformation())
            .map(ProcessExitInformation::getMessage)
            .orElse(null);
    }

    public KubernetesExecutionErrorListener getErrorListener() {
        return errorListener;
    }
}
