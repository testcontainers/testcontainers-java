package org.testcontainers.utility;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import lombok.SneakyThrows;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ResourceReaperTest {

    @Test
    public void shouldCleanupWithRyuk() {
        Map<String, String> labels = runProcess(processExecutor -> {});

        assertCleanup(labels);
    }

    @Test
    public void shouldCleanupWithJVM() {
        Map<String, String> labels = runProcess(processExecutor -> {
            processExecutor.environment("TESTCONTAINERS_RYUK_DISABLED", "true");
        });

        assertCleanup(labels);
    }

    private void assertCleanup(Map<String, String> labels) {
        DockerClient client = DockerClientFactory.instance().client();
        ConditionFactory awaitFactory = Awaitility
            .await()
            .atMost(Duration.ofMinutes(1))
            .pollInterval(Duration.ofSeconds(1));

        List<String> labelValues = labels
            .entrySet()
            .stream()
            .map(it -> it.getKey() + "=" + it.getValue())
            .collect(Collectors.toList());

        awaitFactory.untilAsserted(() -> {
            assertThat(client.listContainersCmd().withFilter("label", labelValues).withShowAll(true).exec()).isEmpty();
        });

        awaitFactory.untilAsserted(() -> {
            assertThat(client.listNetworksCmd().withFilter("label", labelValues).exec()).isEmpty();
        });

        awaitFactory.untilAsserted(() -> {
            assertThat(client.listVolumesCmd().withFilter("label", labelValues).exec().getVolumes()).isEmpty();
        });
    }

    @SneakyThrows
    private Map<String, String> runProcess(Consumer<ProcessExecutor> processExecutorConsumer) {
        ProcessExecutor processExecutor = new ProcessExecutor(
            new File(System.getProperty("java.home")).toPath().resolve("bin").resolve("java").toString(),
            "-ea",
            "-classpath",
            System.getProperty("java.class.path"),
            SimpleUsage.class.getName()
        );
        processExecutor.readOutput(true);
        processExecutor.redirectOutput(System.out);
        processExecutor.redirectError(System.err);
        processExecutorConsumer.accept(processExecutor);

        ProcessResult result = processExecutor.execute();
        assertThat(result.getExitValue()).isEqualTo(0);

        String labelsJson = Stream
            .of(result.outputUTF8().split("\n"))
            .filter(it -> it.startsWith(SimpleUsage.LABELS_MARKER))
            .map(it -> it.substring(SimpleUsage.LABELS_MARKER.length()))
            .findFirst()
            .get();

        return new ObjectMapper().readValue(labelsJson, Map.class);
    }

    public static class SimpleUsage {

        static final String LABELS_MARKER = "LABELS:";

        @SneakyThrows
        @SuppressWarnings("deprecation")
        public static void main(String[] args) {
            System.out.println(
                LABELS_MARKER + new ObjectMapper().writeValueAsString(ResourceReaper.instance().getLabels())
            );

            GenericContainer<?> container = new GenericContainer<>("testcontainers/helloworld:1.1.0")
                .withNetwork(org.testcontainers.containers.Network.newNetwork())
                .withExposedPorts(8080);

            container.start();
        }
    }
}
