package org.testcontainers.containers;

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
    }
}
