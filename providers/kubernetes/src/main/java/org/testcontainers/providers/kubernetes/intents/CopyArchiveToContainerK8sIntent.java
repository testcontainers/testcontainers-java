package org.testcontainers.providers.kubernetes.intents;

import io.fabric8.kubernetes.api.model.Pod;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.testcontainers.controller.intents.CopyArchiveToContainerIntent;
import org.testcontainers.providers.kubernetes.KubernetesContext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        File tempDir = Files.createTempDirectory("testcontainers-tmp").toFile().getCanonicalFile();
        Path localRootPath = tempDir.toPath();
        Path remoteRootPath = Paths.get(remotePath);
        try(TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(tarInputStream)) {
            TarArchiveEntry entry;
            while((entry = tarArchiveInputStream.getNextTarEntry()) != null) {
                Path relativePath = Paths.get(entry.getName());
                Path localAbsolutePath = localRootPath.resolve(relativePath);
                Path remoteAbsolutePath = remoteRootPath.resolve(relativePath);

                if(entry.isFile()) {
                    try(FileOutputStream fos = new FileOutputStream(localAbsolutePath.toFile())) {
                        IOUtils.copy(tarArchiveInputStream, fos);
                    }
                } else if(entry.isDirectory()) {
                    Files.createDirectories(localAbsolutePath);
                }

                log.debug("Copying tar entry '{}'", entry.getName());
            }
        }




        ctx.getClient().pods()
            .inNamespace(pod.getMetadata().getNamespace())
            .withName(pod.getMetadata().getName())
            .dir(remotePath)
            .upload(localRootPath);

        return;
    }
}
