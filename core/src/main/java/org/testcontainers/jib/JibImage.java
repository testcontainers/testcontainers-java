package org.testcontainers.jib;

import com.google.cloud.tools.jib.api.Containerizer;
import com.google.cloud.tools.jib.api.DockerClient;
import com.google.cloud.tools.jib.api.DockerDaemonImage;
import com.google.cloud.tools.jib.api.Jib;
import com.google.cloud.tools.jib.api.JibContainer;
import com.google.cloud.tools.jib.api.JibContainerBuilder;
import lombok.SneakyThrows;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.LazyFuture;
import org.testcontainers.utility.ResourceReaper;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JibImage extends LazyFuture<String> {

    private final DockerClient dockerClient = JibDockerClient.instance();

    private static final Map<String, String> DEFAULT_LABELS = Stream
        .of(
            DockerClientFactory.DEFAULT_LABELS.entrySet().stream(),
            ResourceReaper.instance().getLabels().entrySet().stream()
        )
        .flatMap(Function.identity())
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    private final String baseImage;

    private final Function<JibContainerBuilder, JibContainerBuilder> jibContainerBuilderFn;

    public JibImage(String baseImage, Function<JibContainerBuilder, JibContainerBuilder> jibContainerBuilderFn) {
        this.baseImage = baseImage;
        this.jibContainerBuilderFn = jibContainerBuilderFn;
    }

    @SneakyThrows
    @Override
    protected String resolve() {
        JibContainerBuilder containerBuilder = Jib.from(this.dockerClient, DockerDaemonImage.named(this.baseImage));
        Function<JibContainerBuilder, JibContainerBuilder> applyLabelsFn = jibContainerBuilder -> {
            for (Map.Entry<String, String> entry : DEFAULT_LABELS.entrySet()) {
                jibContainerBuilder.addLabel(entry.getKey(), entry.getValue());
            }
            return jibContainerBuilder;
        };
        JibContainer jibContainer =
            this.jibContainerBuilderFn.andThen(applyLabelsFn)
                .apply(containerBuilder)
                .containerize(
                    Containerizer.to(this.dockerClient, DockerDaemonImage.named(Base58.randomString(8).toLowerCase()))
                );
        return jibContainer.getTargetImage().toString();
    }
}
