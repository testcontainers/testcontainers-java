package org.testcontainers.providers.kubernetes.intents;

import com.github.dockerjava.api.async.ResultCallback;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.controller.intents.BuildImageIntent;
import org.testcontainers.controller.intents.BuildResultItem;
import org.testcontainers.providers.kubernetes.KubernetesContext;
import org.testcontainers.providers.kubernetes.model.KanikoBuildParams;
import org.testcontainers.providers.kubernetes.worker.BuildImageWorker;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BuildImageK8sIntent implements BuildImageIntent {
    private final KubernetesContext ctx;
    private final InputStream in;

    private final KanikoBuildParams buildParams = new KanikoBuildParams();



    public BuildImageK8sIntent(
        KubernetesContext ctx,
        InputStream in
    ) {
        this.ctx = ctx;
        this.in = in;
    }

    @Override
    public BuildImageIntent withTag(String tag) {
        buildParams.setTag(tag);
        return this;
    }

    @Override
    public BuildImageIntent withDockerfilePath(String dockerFilePath) {
        buildParams.setDockerFile(Paths.get(dockerFilePath));
        return this;
    }

    @Override
    public BuildImageIntent withDockerfile(File dockerfile) {
        buildParams.setDockerFile(dockerfile.toPath());
        return this;
    }

    @Override
    public BuildImageIntent withPull(boolean enablePull) {
        buildParams.setPullEnabled(enablePull);
        return this;
    }

    @Override
    public BuildImageIntent withBuildArg(String name, String value) {
        buildParams.getBuildArgs().put(name, value);
        return this;
    }

    @Override
    public BuildImageIntent withDisabledCache(boolean disabledCache) {
        buildParams.setDisabledCache(disabledCache);
        return this;
    }

    @Override
    public BuildImageIntent withDisabledPush(boolean disabledPush) {
        buildParams.setDisabledPush(disabledPush);
        return this;
    }

    @Override
    public @NotNull Map<String, String> getLabels() {
        Map<String, String> labels = buildParams.getLabels();
        if (labels == null) {
            return Collections.emptyMap();
        }
        return labels;
    }

    @Override
    public BuildImageIntent withLabels(Map<String, String> labels) {
        buildParams.setLabels(labels);
        return this;
    }


    private Container buildKaniko() {
        return new ContainerBuilder()
            .withName("kaniko")
            .withImage("gcr.io/kaniko-project/executor:v1.6.0-debug") // TODO: Configurable
            .withCommand("cat")
            .withTty(true)
            .build();
    }

    @Override
    @SneakyThrows
    public <T extends ResultCallback<BuildResultItem>> T perform(T resultCallback) {
        Map<String, String> identifierLabels = new HashMap<>();
        identifierLabels.put("testcontainers-uuid", UUID.randomUUID().toString());
        // @formatter:off
        Pod pod = new PodBuilder()
            .editOrNewMetadata()
                .withGenerateName("testcontainers-")
                .withNamespace(ctx.getNamespaceProvider().getNamespace())
                .withLabels(identifierLabels)
            .endMetadata()
            .editOrNewSpec()
                .addNewContainerLike(buildKaniko())
                .endContainer()
            .endSpec()
            .build();
        // @formatter:on

        log.info("Starting build container");
        Pod createdPod = ctx.getClient().pods().create(pod);
        ctx.getClient().pods()
            .inNamespace(createdPod.getMetadata().getNamespace())
            .withName(createdPod.getMetadata().getName())
            .waitUntilReady(3, TimeUnit.MINUTES); // TODO: Configurable

        BuildImageWorker worker = new BuildImageWorker(
            ctx,
            in,
            createdPod,
            buildParams,
            resultCallback
        );
        worker.start();


        return resultCallback;
    }


}
