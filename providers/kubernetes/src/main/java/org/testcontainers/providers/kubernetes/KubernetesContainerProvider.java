package org.testcontainers.providers.kubernetes;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.ContainerProvider;
import org.testcontainers.controller.ContainerProviderInitParams;
import org.testcontainers.controller.configuration.DefaultConfigurationSource;
import org.testcontainers.providers.kubernetes.configuration.KubernetesConfiguration;

import javax.swing.text.html.Option;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class KubernetesContainerProvider implements ContainerProvider {

    private static final String PROVIDER_IDENTIFIER = "kubernetes";

    private KubernetesConfiguration configuration = new KubernetesConfiguration(new DefaultConfigurationSource());
    private final NamespaceTemplateRenderer namespaceTemplateRenderer = new NamespaceTemplateRenderer();

    private KubernetesContainerController instance = null;

    @Override
    public ContainerProvider init(ContainerProviderInitParams params) {
        configuration = new KubernetesConfiguration(params.getConfigurationSource());
        return this;
    }

    @Override
    public ContainerController lazyController() {
        return controller(); // TODO: Implement actual lazy controller
    }

    @Override
    @SneakyThrows
    public synchronized ContainerController controller() {
        if(instance == null) {
            instance = createController();
        }
        return instance;
    }

    private KubernetesContainerController createController() {
        KubernetesContext ctx = buildKubernetesContext();

        Optional<String> nodePortAddress = configuration.getNodePortAddress();
        nodePortAddress.ifPresent(ctx::withNodePortAddress);

        String requiredNamespace = ctx.getNamespaceProvider().getNamespace();

        Namespace existingNamespace = ctx.getClient()
            .namespaces()
            .withName(requiredNamespace)
            .get();

        ctx.getClient().getConfiguration().setNamespace(requiredNamespace);

        KubernetesContainerController controller = new KubernetesContainerController(
            ctx
        );

        if(existingNamespace == null) {
            Optional<Map<String, String>> configuredLabels = configuration.getNamespaceLabels();
            Optional<Map<String, String>> configuredAnnotations = configuration.getNamespaceAnnotations();
            Namespace createdNamespace = ctx.getClient()
                .namespaces()
                .create(
                    new NamespaceBuilder()
                        .editOrNewMetadata()
                        .withName(requiredNamespace)
                        .withLabels(configuredLabels.orElseGet(Collections::emptyMap))
                        .withAnnotations(configuredAnnotations.orElseGet(Collections::emptyMap))
                        .endMetadata()
                        .build()
                );
            controller.getResourceReaper().registerNamespaceForCleanup(createdNamespace);
        }

        return controller;
    }

    private KubernetesContext buildKubernetesContext() {
        KubernetesClient client = new DefaultKubernetesClient();

        String namespacePattern = namespaceTemplateRenderer.render(
            configuration.getNamespacePattern()
                .orElse(client.getNamespace())
        );

        NamespaceProvider namespaceProvider = new StaticNamespaceProvider(namespacePattern);

        return new KubernetesContext(client, namespaceProvider);
    }

    @Override
    public boolean supportsExecution() {
        return true;
    }

    @Override
    public boolean isFileMountingSupported() {
        return true;
    }

    @Override
    public String getRandomImageName() {
        return "docker.cluster.lise.de/testcontainers/" + UUID.randomUUID().toString().toLowerCase(); // TODO: Implement temp registry
    }

    @Override
    public String getIdentifier() {
        return PROVIDER_IDENTIFIER;
    }

    @Override
    public boolean isAvailable() {
        try {
            KubernetesClient kc = new DefaultKubernetesClient();
            return kc.getVersion() != null;
        } catch (Throwable th) {
            log.debug("Kubernetes is not available. See stack trace for more details.", th);
            return false;
        }
    }
}
