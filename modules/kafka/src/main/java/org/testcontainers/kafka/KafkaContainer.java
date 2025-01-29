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
 * Testcontainers implementation for Apache Kafka.
 * <p>
 * Supported image: {@code apache/kafka}, {@code apache/kafka-native}
 * <p>
 * Exposed ports: 9092
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("apache/kafka");

    private static final DockerImageName APACHE_KAFKA_NATIVE_IMAGE_NAME = DockerImageName.parse("apache/kafka-native");

    private static final int KAFKA_PORT = 9092;

    private static final String STARTER_SCRIPT = "/tmp/testcontainers_start.sh";

    private final Set<String> listeners = new HashSet<>();

    private final Set<Supplier<String>> advertisedListeners = new HashSet<>();

    public KafkaContainer(String imageName) {
        this(DockerImageName.parse(imageName));
    }

    public KafkaContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, APACHE_KAFKA_NATIVE_IMAGE_NAME);

        withExposedPorts(KAFKA_PORT);
        withEnv(KafkaHelper.envVars());

        withCommand(KafkaHelper.COMMAND);
        waitingFor(KafkaHelper.WAIT_STRATEGY);
    }

    @Override
    protected void configure() {
        KafkaHelper.resolveListeners(this, this.listeners);
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

        command += "/etc/kafka/docker/run \n";
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }

    /**
     * Add a listener in the format {@code host:port}.
     * Host will be included as a network alias.
     * <p>
     * Use it to register additional connections to the Kafka broker within the same container network.
     * <p>
     * The listener will be added to the list of default listeners.
     * <p>
     * Default listeners:
     * <ul>
     *     <li>0.0.0.0:9092</li>
     *     <li>0.0.0.0:9093</li>
     *     <li>0.0.0.0:9094</li>
     * </ul>
     * <p>
     * The listener will be added to the list of default advertised listeners.
     * <p>
     * Default advertised listeners:
     * <ul>
     *      <li>{@code container.getConfig().getHostName():9092}</li>
     *      <li>{@code container.getHost():container.getMappedPort(9093)}</li>
     * </ul>
     * @param listener a listener with format {@code host:port}
     * @return this {@link KafkaContainer} instance
     */
    public KafkaContainer withListener(String listener) {
        this.listeners.add(listener);
        this.advertisedListeners.add(() -> listener);
        return this;
    }

    /**
     * Add a listener in the format {@code host:port} and a {@link Supplier} for the advertised listener.
     * Host from listener will be included as a network alias.
     * <p>
     * Use it to register additional connections to the Kafka broker from outside the container network
     * <p>
     * The listener will be added to the list of default listeners.
     * <p>
     * Default listeners:
     * <ul>
     *     <li>0.0.0.0:9092</li>
     *     <li>0.0.0.0:9093</li>
     *     <li>0.0.0.0:9094</li>
     * </ul>
     * <p>
     * The {@link Supplier} will be added to the list of default advertised listeners.
     * <p>
     * Default advertised listeners:
     * <ul>
     *      <li>{@code container.getConfig().getHostName():9092}</li>
     *      <li>{@code container.getHost():container.getMappedPort(9093)}</li>
     * </ul>
     * @param listener a supplier that will provide a listener
     * @param advertisedListener a supplier that will provide a listener
     * @return this {@link KafkaContainer} instance
     */
    public KafkaContainer withListener(String listener, Supplier<String> advertisedListener) {
        this.listeners.add(listener);
        this.advertisedListeners.add(advertisedListener);
        return this;
    }

    public String getBootstrapServers() {
        return String.format("%s:%s", getHost(), getMappedPort(KAFKA_PORT));
    }
}
