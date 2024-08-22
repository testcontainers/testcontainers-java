package org.testcontainers.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Testcontainers implementation for Confluent Kafka.
 * <p>
 * Supported image: {@code confluentinc/cp-kafka}
 * <p>
 * Exposed ports: 9092
 */
public class ConfluentKafkaContainer extends GenericContainer<ConfluentKafkaContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("confluentinc/cp-kafka");

    private final Set<String> listeners = new HashSet<>();

    private final Set<Supplier<String>> advertisedListeners = new HashSet<>();

    public ConfluentKafkaContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public ConfluentKafkaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(KafkaHelper.KAFKA_PORT);
        withEnv(KafkaHelper.envVars());

        withCommand(KafkaHelper.COMMAND);
        waitingFor(KafkaHelper.WAIT_STRATEGY);
    }

    @Override
    protected void configure() {
        KafkaHelper.resolveListeners(this, this.listeners);

        String firstNetworkAlias = getNetworkAliases().stream().findFirst().orElse(null);
        String networkAlias = getNetwork() != null ? firstNetworkAlias : "localhost";
        String controllerQuorumVoters = String.format("%s@%s:9094", getEnvMap().get("KAFKA_NODE_ID"), networkAlias);
        withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", controllerQuorumVoters);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        String brokerAdvertisedListener = String.format(
            "BROKER://%s:%s",
            containerInfo.getConfig().getHostName(),
            "9093"
        );
        List<String> advertisedListeners = new ArrayList<>();
        advertisedListeners.add("PLAINTEXT://" + getBootstrapServers());
        advertisedListeners.add(brokerAdvertisedListener);

        advertisedListeners.addAll(KafkaHelper.resolveAdvertisedListeners(this.advertisedListeners));
        String kafkaAdvertisedListeners = String.join(",", advertisedListeners);

        String command = "#!/bin/bash\n";
        // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
        command += String.format("export KAFKA_ADVERTISED_LISTENERS=%s\n", kafkaAdvertisedListeners);

        command += "/etc/confluent/docker/run \n";
        copyFileToContainer(Transferable.of(command, 0777), KafkaHelper.STARTER_SCRIPT);
    }

    public ConfluentKafkaContainer withListener(String listener) {
        this.listeners.add(listener);
        this.advertisedListeners.add(() -> listener);
        return this;
    }

    public ConfluentKafkaContainer withListener(String listener, Supplier<String> advertisedListener) {
        this.listeners.add(listener);
        this.advertisedListeners.add(advertisedListener);
        return this;
    }

    public String getBootstrapServers() {
        return String.format("%s:%s", getHost(), getMappedPort(KafkaHelper.KAFKA_PORT));
    }
}
