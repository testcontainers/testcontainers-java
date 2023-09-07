package org.testcontainers.examples;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.BlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Examples with Hazelcast using both a single container and a cluster with two containers.
 */
class HazelcastTest {

    // Hazelcast values
    private static final String HZ_IMAGE_NAME = "hazelcast/hazelcast:5.2.0";

    private static final String HZ_CLUSTERNAME_ENV_NAME = "HZ_CLUSTERNAME";

    private static final String HZ_NETWORK_JOIN_AZURE_ENABLED_ENV_NAME = "HZ_NETWORK_JOIN_AZURE_ENABLED";

    private static final String HZ_NETWORK_JOIN_MULTICAST_ENABLED_ENV_NAME = "HZ_NETWORK_JOIN_MULTICAST_ENABLED";

    private static final int DEFAULT_EXPOSED_PORT = 5701;

    // Test values
    private static final String CLUSTER_STARTUP_LOG_MESSAGE_REGEX = ".*Members \\{size:2.*";

    private static final String HOST_PORT_SEPARATOR = ":";

    private static final String TEST_QUEUE_NAME = "test-queue";

    private static final String TEST_CLUSTER_NAME = "test-cluster";

    private static final String TEST_VALUE = "Hello!";

    private static final String FALSE_VALUE = "false";

    private static final String TRUE_VALUE = "true";

    @AfterEach
    void cleanUp() {
        HazelcastClient.shutdownAll();
    }

    @Test
    void singleHazelcastContainer() {
        try (
            GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(HZ_IMAGE_NAME))
                .withExposedPorts(DEFAULT_EXPOSED_PORT)
        ) {
            container.start();
            assertThat(container.isRunning()).isTrue();

            ClientConfig clientConfig = new ClientConfig();
            clientConfig
                .getNetworkConfig()
                .addAddress(container.getHost() + HOST_PORT_SEPARATOR + container.getFirstMappedPort());
            HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

            BlockingQueue<String> queue = client.getQueue(TEST_QUEUE_NAME);
            queue.put(TEST_VALUE);
            assertThat(queue.take()).isEqualTo(TEST_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during singleHazelcastContainer test", e);
        }
    }

    @Test
    void hazelcastCluster() {
        Network network = Network.newNetwork();
        try (
            GenericContainer<?> container1 = new GenericContainer<>(DockerImageName.parse(HZ_IMAGE_NAME))
                .withExposedPorts(DEFAULT_EXPOSED_PORT)
                .withEnv(HZ_CLUSTERNAME_ENV_NAME, TEST_CLUSTER_NAME)
                // Flags necessary to run on Github Actions which runs on an Azure VM
                .withEnv(HZ_NETWORK_JOIN_AZURE_ENABLED_ENV_NAME, FALSE_VALUE)
                .withEnv(HZ_NETWORK_JOIN_MULTICAST_ENABLED_ENV_NAME, TRUE_VALUE)
                .waitingFor(Wait.forLogMessage(CLUSTER_STARTUP_LOG_MESSAGE_REGEX, 1))
                .withNetwork(network);
            GenericContainer<?> container2 = new GenericContainer<>(DockerImageName.parse(HZ_IMAGE_NAME))
                .withExposedPorts(DEFAULT_EXPOSED_PORT)
                .withEnv(HZ_CLUSTERNAME_ENV_NAME, TEST_CLUSTER_NAME)
                // Flags necessary to run on Github Actions which runs on an Azure VM
                .withEnv(HZ_NETWORK_JOIN_AZURE_ENABLED_ENV_NAME, FALSE_VALUE)
                .withEnv(HZ_NETWORK_JOIN_MULTICAST_ENABLED_ENV_NAME, TRUE_VALUE)
                .waitingFor(Wait.forLogMessage(CLUSTER_STARTUP_LOG_MESSAGE_REGEX, 1))
                .withNetwork(network)
        ) {
            Startables.deepStart(container1, container2).join();
            assertThat(container1.isRunning()).isTrue();
            assertThat(container2.isRunning()).isTrue();

            ClientConfig clientConfig = new ClientConfig();
            clientConfig
                .setClusterName(TEST_CLUSTER_NAME)
                .getNetworkConfig()
                // Uncomment the next line to remove the "WARNING: ...Could not connect to member..." message
                //.setSmartRouting(false)
                .addAddress(container1.getHost() + HOST_PORT_SEPARATOR + container1.getFirstMappedPort())
                .addAddress(container2.getHost() + HOST_PORT_SEPARATOR + container2.getFirstMappedPort());

            HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

            assertThat(client.getCluster().getMembers()).hasSize(2);

            BlockingQueue<String> queue = client.getQueue(TEST_QUEUE_NAME);
            queue.put(TEST_VALUE);

            assertThat(queue.take()).isEqualTo(TEST_VALUE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during hazelcastCluster test", e);
        }
    }
}
