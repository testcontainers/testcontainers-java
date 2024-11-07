package org.testcontainers.kafka;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class KafkaHelper {

    private static final String DEFAULT_INTERNAL_TOPIC_RF = "1";

    private static final String DEFAULT_CLUSTER_ID = "4L6g3nShT-eMCtK--X86sw";

    private static final String PROTOCOL_PREFIX = "TC";

    static final int KAFKA_PORT = 9092;

    static final String STARTER_SCRIPT = "/tmp/testcontainers_start.sh";

    static final String[] COMMAND = {
        "sh",
        "-c",
        "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT,
    };

    static final WaitStrategy WAIT_STRATEGY = Wait.forLogMessage(".*Transitioning from RECOVERY to RUNNING.*", 1);

    static Map<String, String> envVars() {
        Map<String, String> envVars = new HashMap<>();
        envVars.put("CLUSTER_ID", DEFAULT_CLUSTER_ID);

        envVars.put("KAFKA_LISTENERS", "PLAINTEXT://:" + KAFKA_PORT + ",BROKER://:9093,CONTROLLER://:9094");
        envVars.put(
            "KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
            "BROKER:PLAINTEXT,PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT"
        );
        envVars.put("KAFKA_INTER_BROKER_LISTENER_NAME", "BROKER");
        envVars.put("KAFKA_PROCESS_ROLES", "broker,controller");
        envVars.put("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER");

        envVars.put("KAFKA_NODE_ID", "1");
        envVars.put("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", DEFAULT_INTERNAL_TOPIC_RF);
        envVars.put("KAFKA_LOG_FLUSH_INTERVAL_MESSAGES", Long.MAX_VALUE + "");
        envVars.put("KAFKA_GROUP_INITIAL_REBALANCE_DELAY_MS", "0");
        return envVars;
    }

    static void resolveListeners(GenericContainer<?> kafkaContainer, Set<String> listenersSuppliers) {
        Set<String> listeners = Arrays
            .stream(kafkaContainer.getEnvMap().get("KAFKA_LISTENERS").split(","))
            .collect(Collectors.toSet());
        Set<String> listenerSecurityProtocolMap = Arrays
            .stream(kafkaContainer.getEnvMap().get("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP").split(","))
            .collect(Collectors.toSet());

        List<String> listenersToTransform = new ArrayList<>(listenersSuppliers);
        for (int i = 0; i < listenersToTransform.size(); i++) {
            String protocol = String.format("%s-%d", PROTOCOL_PREFIX, i);
            String listener = listenersToTransform.get(i);
            String listenerHost = listener.split(":")[0];
            String listenerPort = listener.split(":")[1];
            String listenerProtocol = String.format("%s://%s:%s", protocol, listenerHost, listenerPort);
            String protocolMap = String.format("%s:PLAINTEXT", protocol);
            listeners.add(listenerProtocol);
            listenerSecurityProtocolMap.add(protocolMap);

            String host = listener.split(":")[0];
            kafkaContainer.withNetworkAliases(host);
        }

        String kafkaListeners = String.join(",", listeners);
        String kafkaListenerSecurityProtocolMap = String.join(",", listenerSecurityProtocolMap);

        kafkaContainer.getEnvMap().put("KAFKA_LISTENERS", kafkaListeners);
        kafkaContainer.getEnvMap().put("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", kafkaListenerSecurityProtocolMap);
    }

    static List<String> resolveAdvertisedListeners(Set<Supplier<String>> listenerSuppliers) {
        List<String> advertisedListeners = new ArrayList<>();
        List<Supplier<String>> listenersToTransform = new ArrayList<>(listenerSuppliers);
        for (int i = 0; i < listenersToTransform.size(); i++) {
            Supplier<String> listenerSupplier = listenersToTransform.get(i);
            String protocol = String.format("%s-%d", PROTOCOL_PREFIX, i);
            String listener = listenerSupplier.get();
            String listenerProtocol = String.format("%s://%s", protocol, listener);
            advertisedListeners.add(listenerProtocol);
        }
        return advertisedListeners;
    }
}
