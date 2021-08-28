package org.testcontainers.providers.kubernetes.execution;

import io.fabric8.kubernetes.client.dsl.ExecListener;
import lombok.SneakyThrows;
import okhttp3.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class KubernetesExecutionListener implements ExecListener {

    private final CountDownLatch execLatch = new CountDownLatch(1);

    private final List<KubernetesExecutionLogAdapter<?>> logAdapters = new ArrayList<>();

    private Integer exitCode;
    private String reason;


    public KubernetesExecutionListener withLogAdapters(KubernetesExecutionLogAdapter<?>... adapters) {
        logAdapters.addAll(Arrays.asList(adapters));
        return this;
    }

    @SneakyThrows
    private void closeAdapters() {
        for(KubernetesExecutionLogAdapter<?> adapter : logAdapters) {
            adapter.close();
        }
    }

    @Override
    public void onOpen(Response response) {
    }

    @Override
    public void onFailure(Throwable t, Response response) {
        execLatch.countDown();
        closeAdapters();
    }

    @Override
    public void onClose(int code, String reason) {
        this.exitCode = (code - 1000); // TODO Get real exit code
        this.reason = reason;
        execLatch.countDown();
        closeAdapters();
    }

    @SneakyThrows
    public void waitForCompletion() {
        execLatch.await();
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getReason() {
        return reason;
    }
}
