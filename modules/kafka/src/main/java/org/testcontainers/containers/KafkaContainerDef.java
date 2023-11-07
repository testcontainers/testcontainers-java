package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class KafkaContainerDef extends BaseContainerDef {

    public static final int KAFKA_PORT = 9093;

    public static final int ZOOKEEPER_PORT = 2181;

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    public static final String DEFAULT_CLUSTER_ID = "4L6g3nShT-eMCtK--X86sw";

    private static final String PROTOCOL_PREFIX = "TC";

    private final Set<Supplier<String>> listeners = new HashSet<>();

    private String clusterId = DEFAULT_CLUSTER_ID;

    private boolean kraftEnabled;

    private String externalZookeeperConnect;

    KafkaContainerDef() {
        // Use two listeners with different names, it will force Kafka to communicate with itself via internal
        // listener when KAFKA_INTER_BROKER_LISTENER_NAME is set, otherwise Kafka will try to use the advertised listener
        addEnvVar("KAFKA_LISTENERS", "PLAINTEXT://0.0.0.0:" + KAFKA_PORT + ",BROKER://0.0.0.0:9092");
        addEnvVar("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT");
        addEnvVar("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");

        addEnvVar("KAFKA_BROKER_ID", "1");
        addEnvVar("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        addEnvVar("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF);
        addEnvVar("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        addEnvVar("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF);
        addEnvVar("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        addEnvVar("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");

        addExposedTcpPort(KAFKA_PORT);

        setEntrypoint("sh");
        setCommand("-c", "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT);

        setWaitStrategy(Wait.forLogMessage(".*\\[KafkaServer id=\\d+\\] started.*", 1));
    }

    void withListener(Supplier<String> listenerSupplier) {
        this.listeners.add(listenerSupplier);
    }

    void withClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    void withKraftEnabled() {
        this.kraftEnabled = true;
    }

    void withExternalZookeeperConnect(String externalZookeeperConnect) {
        this.externalZookeeperConnect = externalZookeeperConnect;
    }

    @Override
    protected StartedKafkaContainer toStarted(ContainerState containerState) {
        return new KafkaStarted(containerState);
    }

    class KafkaStarted extends BaseContainerDef.Started implements StartedKafkaContainer, ContainerLifecycleHooks {

        public KafkaStarted(ContainerState containerState) {
            super(containerState);
        }

        @Override
        public String getBootstrapServers() {
            return String.format("PLAINTEXT://%s:%s", getHost(), getMappedPort(KAFKA_PORT));
        }

        @Override
        public void configure() {
            resolveListeners();

            if (kraftEnabled) {
                configureKraft();
            } else {
                configureZookeeper();
            }
        }

        protected void configureKraft() {
            envVars.computeIfAbsent("CLUSTER_ID", key -> clusterId);
            envVars.computeIfAbsent("KAFKA_NODE_ID", key -> getEnvVars().get("KAFKA_BROKER_ID"));
            addEnvVar(
                "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                String.format("%s,CONTROLLER:PLAINTEXT", getEnvVars().get("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP"))
            );
            addEnvVar(
                "KAFKA_LISTENERS",
                String.format("%s,CONTROLLER://0.0.0.0:9094", getEnvVars().get("KAFKA_LISTENERS"))
            );
            addEnvVar("KAFKA_PROCESS_ROLES", "broker,controller");

            String firstNetworkAlias = getNetworkAliases().stream().findFirst().orElse(null);
            String networkAlias = getNetwork() != null ? firstNetworkAlias : "localhost";
            String controllerQuorumVoters = String.format(
                "%s@%s:9094",
                getEnvVars().get("KAFKA_NODE_ID"),
                networkAlias
            );
            envVars.computeIfAbsent("KAFKA_CONTROLLER_QUORUM_VOTERS", key -> controllerQuorumVoters);
            addEnvVar("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER");

            setWaitStrategy(Wait.forLogMessage(".*Transitioning from RECOVERY to RUNNING.*", 1));
        }

        protected void configureZookeeper() {
            if (externalZookeeperConnect == null) {
                addExposedTcpPort(ZOOKEEPER_PORT);
                addEnvVar("KAFKA_ZOOKEEPER_CONNECT", "localhost:" + ZOOKEEPER_PORT);
            } else {
                addEnvVar("KAFKA_ZOOKEEPER_CONNECT", externalZookeeperConnect);
            }
        }

        private void resolveListeners() {
            Set<String> allListeners = Arrays
                .stream(envVars.get("KAFKA_LISTENERS").split(","))
                .collect(Collectors.toSet());
            Set<String> allListenerSecurityProtocolMap = Arrays
                .stream(envVars.get("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP").split(","))
                .collect(Collectors.toSet());

            List<Supplier<String>> listenersToTransform = new ArrayList<>(listeners);
            for (int i = 0; i < listenersToTransform.size(); i++) {
                Supplier<String> listenerSupplier = listenersToTransform.get(i);
                String protocol = String.format("%s-%d", PROTOCOL_PREFIX, i);
                String listener = listenerSupplier.get();
                String listenerPort = listener.split(":")[1];
                String listenerProtocol = String.format("%s://0.0.0.0:%s", protocol, listenerPort);
                String protocolMap = String.format("%s:PLAINTEXT", protocol);
                allListeners.add(listenerProtocol);
                allListenerSecurityProtocolMap.add(protocolMap);

                String host = listener.split(":")[0];
                addNetworkAlias(host);
            }

            String kafkaListeners = String.join(",", allListeners);
            String kafkaListenerSecurityProtocolMap = String.join(",", allListenerSecurityProtocolMap);

            envVars.put("KAFKA_LISTENERS", kafkaListeners);
            envVars.put("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", kafkaListenerSecurityProtocolMap);
        }

        @Override
        public void containerIsStarting(boolean reused) {
            List<String> advertisedListeners = new ArrayList<>();
            advertisedListeners.add(getBootstrapServers());
            advertisedListeners.add(brokerAdvertisedListener(getContainerInfo()));

            List<Supplier<String>> listenersToTransform = new ArrayList<>(listeners);
            for (int i = 0; i < listenersToTransform.size(); i++) {
                Supplier<String> listenerSupplier = listenersToTransform.get(i);
                String protocol = String.format("%s-%d", PROTOCOL_PREFIX, i);
                String listener = listenerSupplier.get();
                String listenerProtocol = String.format("%s://%s", protocol, listener);
                advertisedListeners.add(listenerProtocol);
            }

            String kafkaAdvertisedListeners = String.join(",", advertisedListeners);

            String command = "#!/bin/bash\n";
            // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
            command += String.format("export KAFKA_ADVERTISED_LISTENERS=%s\n", kafkaAdvertisedListeners);

            if (kraftEnabled && isLessThanCP740()) {
                // Optimization: skip the checks
                command += "echo '' > /etc/confluent/docker/ensure \n";
                command += commandKraft();
            }

            if (!kraftEnabled) {
                // Optimization: skip the checks
                command += "echo '' > /etc/confluent/docker/ensure \n";
                command += commandZookeeper();
            }

            // Run the original command
            command += "/etc/confluent/docker/run \n";
            copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
        }
    }

    private String brokerAdvertisedListener(InspectContainerResponse containerInfo) {
        return String.format("BROKER://%s:%s", containerInfo.getConfig().getHostName(), "9092");
    }

    private String commandKraft() {
        String command = "sed -i '/KAFKA_ZOOKEEPER_CONNECT/d' /etc/confluent/docker/configure\n";
        command +=
            "echo 'kafka-storage format --ignore-formatted -t \"" +
            envVars.get("CLUSTER_ID") +
            "\" -c /etc/kafka/kafka.properties' >> /etc/confluent/docker/configure\n";
        return command;
    }

    private String commandZookeeper() {
        String command = "echo 'clientPort=" + ZOOKEEPER_PORT + "' > zookeeper.properties\n";
        command += "echo 'dataDir=/var/lib/zookeeper/data' >> zookeeper.properties\n";
        command += "echo 'dataLogDir=/var/lib/zookeeper/log' >> zookeeper.properties\n";
        command += "zookeeper-server-start zookeeper.properties &\n";
        return command;
    }

    private boolean isLessThanCP740() {
        String actualVersion = DockerImageName.parse(getImage().get()).getVersionPart();
        return new ComparableVersion(actualVersion).isLessThan("7.4.0");
    }
}
