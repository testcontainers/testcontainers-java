package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkTest {

    @Nested
    class WithRules {

        public Network network = Network.newNetwork();

        public GenericContainer<?> foo = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withNetwork(network)
            .withNetworkAliases("foo")
            .withCommand("/bin/sh", "-c", "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done");

        public GenericContainer<?> bar = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withNetwork(network)
            .withCommand("top");

        void testNetworkSupport() throws Exception {
            foo.start();
            bar.start();
            String response = bar.execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
            assertThat(response).as("received response").isEqualTo("yay");
        }
    }

    @Nested
    class WithoutRules {

        @Test
        void testNetworkSupport() throws Exception {
            // useCustomNetwork {
            try (
                Network network = Network.newNetwork();
                GenericContainer<?> foo = new GenericContainer<>(TestImages.TINY_IMAGE)
                    .withNetwork(network)
                    .withNetworkAliases("foo")
                    .withCommand(
                        "/bin/sh",
                        "-c",
                        "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done"
                    );
                GenericContainer<?> bar = new GenericContainer<>(TestImages.TINY_IMAGE)
                    .withNetwork(network)
                    .withCommand("top")
            ) {
                foo.start();
                bar.start();

                String response = bar.execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
                assertThat(response).as("received response").isEqualTo("yay");
            }
            // }
        }

        @Test
        void testBuilder() {
            try (Network network = Network.builder().driver("macvlan").build()) {
                String id = network.getId();
                assertThat(
                    DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(id).exec().getDriver()
                )
                    .as("Flag is set")
                    .isEqualTo("macvlan");
            }
        }

        @Test
        void testModifiers() {
            try (
                Network network = Network.builder().createNetworkCmdModifier(cmd -> cmd.withDriver("macvlan")).build()
            ) {
                String id = network.getId();
                assertThat(
                    DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(id).exec().getDriver()
                )
                    .as("Flag is set")
                    .isEqualTo("macvlan");
            }
        }

        @Test
        void testReusability() {
            try (Network network = Network.newNetwork()) {
                String firstId = network.getId();
                assertThat(DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(firstId).exec())
                    .as("Network exists")
                    .isNotNull();

                network.close();

                assertThat(
                    DockerClientFactory
                        .instance()
                        .client()
                        .inspectNetworkCmd()
                        .withNetworkId(network.getId())
                        .exec()
                        .getId()
                )
                    .as("New network created")
                    .isNotEqualTo(firstId);
            }
        }

        @Test
        void getFirstMappedPortRemainsCorrectAfterSecondaryNetworkAttach() {
            // Regression for https://github.com/testcontainers/testcontainers-java/issues/11779
            try (
                Network primary = Network.newNetwork();
                Network secondary = Network.newNetwork();
                GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                    .withNetwork(primary)
                    .withNetworkAliases("test-host")
                    .withExposedPorts(7077)
                    .withCommand(
                        "/bin/sh",
                        "-c",
                        "while true ; do printf 'HTTP/1.1 200 OK\\n\\nok' | nc -l -p 7077; done"
                    )
            ) {
                container.start();

                Integer mappedPortBefore = container.getFirstMappedPort();
                assertThat(mappedPortBefore).isEqualTo(publishedHostPort(container, 7077));

                DockerClient dockerClient = DockerClientFactory.instance().client();
                dockerClient
                    .connectToNetworkCmd()
                    .withContainerId(container.getContainerId())
                    .withNetworkId(secondary.getId())
                    .exec();

                assertThat(container.getFirstMappedPort())
                    .as("mapped port after secondary network attach matches Docker")
                    .isEqualTo(publishedHostPort(container, 7077));
                assertThat(container.getMappedPort(7077)).isEqualTo(publishedHostPort(container, 7077));
            }
        }

        @Test
        void getMappedPortReflectsDockerRemapAfterNetworkDisconnectReconnect() {
            // Docker reassigns published host ports after network disconnect/reconnect.
            // Cached inspect data must not be used for mapped-port lookups.
            try (
                Network primary = Network.newNetwork();
                Network secondary = Network.newNetwork();
                GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                    .withNetwork(primary)
                    .withExposedPorts(7077)
                    .withCommand(
                        "/bin/sh",
                        "-c",
                        "while true ; do printf 'HTTP/1.1 200 OK\\n\\nok' | nc -l -p 7077; done"
                    )
            ) {
                container.start();

                Integer mappedPortBefore = container.getMappedPort(7077);
                Integer cachedPortBefore = hostPortFromInspect(container.getContainerInfo(), 7077);
                assertThat(mappedPortBefore).isEqualTo(cachedPortBefore);

                DockerClient dockerClient = DockerClientFactory.instance().client();
                dockerClient
                    .connectToNetworkCmd()
                    .withContainerId(container.getContainerId())
                    .withNetworkId(secondary.getId())
                    .exec();
                dockerClient
                    .disconnectFromNetworkCmd()
                    .withContainerId(container.getContainerId())
                    .withNetworkId(primary.getId())
                    .exec();
                dockerClient
                    .connectToNetworkCmd()
                    .withContainerId(container.getContainerId())
                    .withNetworkId(primary.getId())
                    .exec();

                Integer publishedAfter = publishedHostPort(container, 7077);
                Integer stillCachedFromStart = hostPortFromInspect(container.getContainerInfo(), 7077);

                // When Docker remaps the host port, the start-time cache is stale — getMappedPort
                // must still return Docker's current binding (not the cached one).
                if (!publishedAfter.equals(stillCachedFromStart)) {
                    assertThat(container.getMappedPort(7077))
                        .as("must not return stale cached host port after network remap")
                        .isNotEqualTo(stillCachedFromStart)
                        .isEqualTo(publishedAfter);
                } else {
                    assertThat(container.getMappedPort(7077)).isEqualTo(publishedAfter);
                }
                assertThat(container.getFirstMappedPort()).isEqualTo(publishedAfter);
            }
        }

        private static Integer publishedHostPort(GenericContainer<?> container, int containerPort) {
            InspectContainerResponse inspect = DockerClientFactory
                .instance()
                .client()
                .inspectContainerCmd(container.getContainerId())
                .exec();
            return hostPortFromInspect(inspect, containerPort);
        }

        private static Integer hostPortFromInspect(InspectContainerResponse inspect, int containerPort) {
            Ports.Binding[] bindings = inspect
                .getNetworkSettings()
                .getPorts()
                .getBindings()
                .get(new ExposedPort(containerPort));
            assertThat(bindings).isNotNull().isNotEmpty();
            return Integer.valueOf(bindings[0].getHostPortSpec());
        }
    }
}
