package org.testcontainers.examples;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import org.junit.AfterClass;
import org.junit.Test;
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
public class HazelcastContainerTest {

    // Hazelcast values
    private static final String HZ_IMAGE_NAME = "hazelcast/hazelcast:5.2.0";

    private static final String HZ_CLUSTERNAME_ENV_NAME = "HZ_CLUSTERNAME";

    private static final int DEFAULT_EXPOSED_PORT = 5701;

    private static final String CLUSTER_STARTUP_LOG_MESSAGE_REGEX = ".*Members \\{size:2, ver:2\\}.*";

    // Test values
    private static final String HOST_PORT_SEPARATOR = ":";

    private static final String TEST_QUEUE_NAME = "test-queue";

    private static final String TEST_CLUSTER_NAME = "test-cluster";

    private static final String TEST_VALUE = "Hello!";

    @AfterClass
    public static void cleanUp() {
        HazelcastClient.shutdownAll();
    }

    @Test
    public void singleHazelcastContainer() throws InterruptedException {
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
        }
    }

    @Test
    public void hazelcastCluster() throws InterruptedException {
        Network network = Network.newNetwork();
        try (
            GenericContainer<?> container1 = new GenericContainer<>(DockerImageName.parse(HZ_IMAGE_NAME))
                .withExposedPorts(DEFAULT_EXPOSED_PORT)
                .withEnv(HZ_CLUSTERNAME_ENV_NAME, TEST_CLUSTER_NAME)
                .waitingFor(Wait.forLogMessage(CLUSTER_STARTUP_LOG_MESSAGE_REGEX, 1))
                .withNetwork(network);
            GenericContainer<?> container2 = new GenericContainer<>(DockerImageName.parse(HZ_IMAGE_NAME))
                .withExposedPorts(DEFAULT_EXPOSED_PORT)
                .withEnv(HZ_CLUSTERNAME_ENV_NAME, TEST_CLUSTER_NAME)
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
                .addAddress(container1.getHost() + HOST_PORT_SEPARATOR + container1.getFirstMappedPort());

            HazelcastInstance client = HazelcastClient.newHazelcastClient(clientConfig);

            BlockingQueue<String> queue = client.getQueue(TEST_QUEUE_NAME);
            queue.put(TEST_VALUE);

            assertThat(queue.take()).isEqualTo(TEST_VALUE);
        }
    }
}
