package org.testcontainers.containers;

import org.testcontainers.utility.Base58;

import java.util.stream.Stream;

public class KafkaContainer extends GenericContainer<KafkaContainer> {

    public static final int KAFKA_PORT = 9092;

    public static final int ZOOKEEPER_PORT = 2181;

    protected String externalZookeeperConnect = null;

    protected SocatContainer proxy;

    public KafkaContainer() {
        this("4.0.0");
    }

    public KafkaContainer(String confluentPlatformVersion) {
        super("confluentinc/cp-kafka:" + confluentPlatformVersion);

        withNetwork(Network.newNetwork());
        String myNetworkAlias = "kafka-" + Base58.randomString(6);
        withNetworkAliases(myNetworkAlias);
        withExposedPorts(KAFKA_PORT);

        // Use two listeners with different names, it will force Kafka to communicate with itself via internal
        // listener when KAFKA_INTER_BROKER_LISTENER_NAME is set, otherwise Kafka will try to use the advertised listener
        withEnv("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:9092,BROKER://" + myNetworkAlias + ":9093");
        withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
        withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        withEnv("KAFKA_BROKER_ID", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1");
        withEnv("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", "1");
        withEnv("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
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
        return String.format("PLAINTEXT://%s:%s", proxy.getContainerIpAddress(), proxy.getFirstMappedPort());
    }

    @Override
    public void start() {
        String myNetworkAlias = getNetworkAliases().get(0);
        proxy = new SocatContainer()
                .withNetwork(getNetwork())
                .withTarget(9092, myNetworkAlias)
                .withTarget(2181, myNetworkAlias);

        proxy.start();
        withEnv("KAFKA_ADVERTISED_LISTENERS", "BROKER://" + myNetworkAlias + ":9093,PLAINTEXT://" + proxy.getContainerIpAddress() + ":" + proxy.getFirstMappedPort());

        if (externalZookeeperConnect != null) {
            withEnv("KAFKA_ZOOKEEPER_CONNECT", externalZookeeperConnect);
        } else {
            addExposedPort(ZOOKEEPER_PORT);
            withEnv("KAFKA_ZOOKEEPER_CONNECT", "localhost:2181");
            withClasspathResourceMapping("tc-zookeeper.properties", "/zookeeper.properties", BindMode.READ_ONLY);
            withCommand("sh", "-c", "zookeeper-server-start /zookeeper.properties & /etc/confluent/docker/run");
        }

        super.start();
    }

    @Override
    public void stop() {
        Stream.<Runnable>of(super::stop, proxy::stop).parallel().forEach(Runnable::run);
    }
}