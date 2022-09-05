package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkTest {

    @Test
    public void testNetworkSupport() throws Exception {
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
    public void testBuilder() {
        try (Network network = Network.builder().driver("macvlan").build()) {
            String id = network.getId();
            assertThat(DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(id).exec().getDriver())
                .as("Flag is set")
                .isEqualTo("macvlan");
        }
    }

    @Test
    public void testModifiers() {
        try (Network network = Network.builder().createNetworkCmdModifier(cmd -> cmd.withDriver("macvlan")).build()) {
            String id = network.getId();
            assertThat(DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(id).exec().getDriver())
                .as("Flag is set")
                .isEqualTo("macvlan");
        }
    }

    @Test
    public void testReusability() {
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
