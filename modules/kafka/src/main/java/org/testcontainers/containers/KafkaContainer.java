package org.testcontainers.containers;

import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.concurrent.TimeUnit;

/**
 * This container wraps Confluent Kafka and Zookeeper (optionally)
 *
 */
public class KafkaContainer extends GenericContainer<KafkaContainer> {

    public static final int KAFKA_PORT = 9093;

    public static final int ZOOKEEPER_PORT = 2181;

    private static final int PORT_NOT_ASSIGNED = -1;

    protected String externalZookeeperConnect = null;

    private int port = PORT_NOT_ASSIGNED;

    public KafkaContainer() {
        this("4.0.0");
    }

    public KafkaContainer(String confluentPlatformVersion) {
        super(TestcontainersConfiguration.getInstance().getKafkaImage() + ":" + confluentPlatformVersion);

        withExposedPorts(KAFKA_PORT);

        // Use two listeners with different names, it will force Kafka to communicate with itself via internal
        // listener when KAFKA_INTER_BROKER_LISTENER_NAME is set, otherwise Kafka will try to use the advertised listener
        withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        withEnv("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
    }

    public KafkaContainer withEmbeddedZookeeper() {
        externalZookeeperConnect = null;
        return self();
    }

    public KafkaContainer withExternalZookeeper(String connectString) {
        externalZookeeperConnect = connectString;
        return self();
    }

    public String getBootstrapServers() {
        if (port == PORT_NOT_ASSIGNED) {
            throw new IllegalStateException("You should start Kafka container first");
        }
        return String.format("PLAINTEXT://%s:%s", getContainerIpAddress(), port);
    }

    @Override
    @NonNull
    public synchronized Network getNetwork() {
        if (super.getNetwork() == null) {
            // Backward compatibility
            withNetwork(Network.newNetwork());
        }
        return super.getNetwork();
    }

    @Override
    protected void doStart() {
        withCommand("sleep infinity");

        if (externalZookeeperConnect == null) {
            addExposedPort(ZOOKEEPER_PORT);
        }

        super.doStart();
    }

    @Override
    @SneakyThrows
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);

        port = getMappedPort(KAFKA_PORT);

        final String zookeeperConnect;
        if (externalZookeeperConnect != null) {
            zookeeperConnect = externalZookeeperConnect;
        } else {
            zookeeperConnect = startZookeeper();
        }

        String internalIp = containerInfo.getNetworkSettings().getIpAddress();

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(getContainerId())
            .withCmd("sh", "-c", "" +
                "export KAFKA_ZOOKEEPER_CONNECT=" + zookeeperConnect + "\n" +
                "export KAFKA_ADVERTISED_LISTENERS=" + getBootstrapServers() + "," + String.format("BROKER://%s:9092", internalIp) + "\n" +
                "/etc/confluent/docker/run"
            )
            .exec();

        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback()).awaitStarted(10, TimeUnit.SECONDS);
    }

    @SneakyThrows(InterruptedException.class)
    private String startZookeeper() {
        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(getContainerId())
            .withCmd("sh", "-c", "" +
                "printf 'clientPort=" + ZOOKEEPER_PORT + "\ndataDir=/var/lib/zookeeper/data\ndataLogDir=/var/lib/zookeeper/log' > /zookeeper.properties\n" +
                "zookeeper-server-start /zookeeper.properties\n"
            )
            .exec();

        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(new ExecStartResultCallback()).awaitStarted(10, TimeUnit.SECONDS);

        return "localhost:" + ZOOKEEPER_PORT;
    }
}
