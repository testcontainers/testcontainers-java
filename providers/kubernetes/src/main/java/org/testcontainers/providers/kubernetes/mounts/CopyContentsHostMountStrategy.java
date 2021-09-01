package org.testcontainers.providers.kubernetes.mounts;

import io.fabric8.kubernetes.api.model.ContainerState;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpecFluent;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;
import org.testcontainers.controller.model.BindMode;
import org.testcontainers.controller.model.HostMount;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.execution.NullInputStream;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CopyContentsHostMountStrategy implements KubernetesHostMountStrategy {

    @Nullable
    private List<HostMount> hostMounts;

    @Override
    public KubernetesHostMountStrategy withHostMounts(@Nullable List<HostMount> hostMounts) {
        this.hostMounts = hostMounts;
        return this;
    }

    @Override
    public <T extends PodTemplateSpecFluent.SpecNested<?>> T configure(T templateNestedSpecNested) {
        if (hostMounts == null || hostMounts.isEmpty()) {
            log.debug("No host mounts to process");
            return templateNestedSpecNested;
        }
        for (int i = 0; i < hostMounts.size(); i++) {
            String identifier = getIdentifier(i);
            HostMount hostMount = hostMounts.get(i);
            File file = getFile(hostMount);

            // @formatter:off
            templateNestedSpecNested
                .addNewVolume()
                    .withName(identifier)
                    .withNewEmptyDir()
                    .endEmptyDir()
                .endVolume();

            templateNestedSpecNested
                .addNewInitContainer()
                    .withName(identifier)
                    .withImage("swaglive/sleep:alpine-3.8")
                    .withArgs(
                        "trap 'exit 0' TERM INT; sleep infinity & wait"
                    )
                    .addNewVolumeMount()
                        .withName(identifier)
                        .withMountPath("/mnt/dest")
                    .endVolumeMount()
                .endInitContainer();


            templateNestedSpecNested
                .editFirstContainer()
                    .addNewVolumeMount()
                        .withName(identifier)
                        .withMountPath(hostMount.getMountPoint().getPath())
                        .withReadOnly(hostMount.getMountPoint().getBindMode() == BindMode.READ_ONLY)
                        .withSubPath(file.isFile() ? file.getName() : null)
                    .endVolumeMount()
                .endContainer();
            // @formatter:on


        }
        return templateNestedSpecNested;
    }

    @SneakyThrows
    @Override
    public void apply(KubernetesContext ctx, Pod pod) {
        for (int i = 0; i < hostMounts.size(); i++) {
            String identifier = getIdentifier(i);
            HostMount hostMount = hostMounts.get(i);

            File file = getFile(hostMount);
            boolean isFile = file.isFile();

            log.debug("Waiting for init container");
            ctx.getClient()
                .pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .waitUntilCondition(
                    p -> p.getStatus().getInitContainerStatuses().stream().anyMatch(c -> c.getName().equals(identifier) && Optional.ofNullable(c.getState()).map(ContainerState::getRunning).isPresent()),
                    3,
                    TimeUnit.MINUTES
                );

            log.debug("Uploading mount contents: {}", hostMount.getHostPath());
            if (isFile) {
                ctx.getClient()
                    .pods()
                    .inNamespace(pod.getMetadata().getNamespace())
                    .withName(pod.getMetadata().getName())
                    .inContainer(identifier)
                    .file("/mnt/dest/" + file.getName())
                    .upload(Paths.get(hostMount.getHostPath()));
            } else {
                ctx.getClient()
                    .pods()
                    .inNamespace(pod.getMetadata().getNamespace())
                    .withName(pod.getMetadata().getName())
                    .inContainer(identifier)
                    .dir("/mnt/dest")
                    .upload(Paths.get(hostMount.getHostPath()));
            }


            CountDownLatch latch = new CountDownLatch(1);
            log.debug("Stopping init container");
            ctx.getClient()
                .pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .inContainer(identifier)
                .readingInput(new NullInputStream())
                .usingListener(new ExecListener() {
                    @Override
                    public void onOpen(Response response) {

                    }

                    @Override
                    public void onFailure(Throwable t, Response response) {
                        latch.countDown(); // TODO: Handle error
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        latch.countDown();
                    }
                })
                .exec("kill", "-SIGINT", "1");

            latch.await();
        }
    }

    private File getFile(HostMount hostMount) {
        return Paths.get(hostMount.getHostPath()).toFile();
    }

    private String getIdentifier(int i) {
        return String.format("hostmount-%d", i);
    }
}
