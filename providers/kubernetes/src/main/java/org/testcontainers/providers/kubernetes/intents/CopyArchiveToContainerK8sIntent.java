package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.Pod;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.testcontainers.controller.intents.CopyArchiveToContainerIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.NullInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Slf4j
public class CopyArchiveToContainerK8sIntent implements CopyArchiveToContainerIntent {
    private final KubernetesContext ctx;
    private final Pod pod;

    private InputStream tarInputStream;
    private String remotePath;

    public CopyArchiveToContainerK8sIntent(KubernetesContext ctx, Pod pod) {
        this.ctx = ctx;
        this.pod = pod;
    }

    @Override
    public CopyArchiveToContainerIntent withTarInputStream(InputStream tarInputStream) {
        this.tarInputStream = tarInputStream;
        return this;
    }

    @Override
    public CopyArchiveToContainerIntent withRemotePath(String remotePath) {
        this.remotePath = remotePath;
        return this;
    }

    @Override
    @SneakyThrows
    public void perform() {
        File tempFile = File.createTempFile("testcontainers", null); // TODO: Remove temp file
        try(FileOutputStream fos = new FileOutputStream(tempFile)) {
            IOUtils.copy(tarInputStream, fos);
        }

        ctx.getClient()
            .pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .file("/tmp/testcontainers.tar")
            .upload(tempFile.toPath());

        ctx.getClient()
            .pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .readingInput(new NullInputStream())
            .exec("tar", "-xvf", "/tmp/testcontainers.tar", "-C", remotePath);

        return;
    }
}
