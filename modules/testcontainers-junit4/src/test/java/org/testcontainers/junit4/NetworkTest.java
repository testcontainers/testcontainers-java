package org.testcontainers.junit4;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class NetworkTest {

    private static DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.16");

    @Rule
    public NetworkRule network = new NetworkRule(Network.newNetwork());

    @Rule
    public ContainerRule<GenericContainer<?>> foo = new ContainerRule<>(
        new GenericContainer<>(TINY_IMAGE)
            .withNetwork(network.get())
            .withNetworkAliases("foo")
            .withCommand("/bin/sh", "-c", "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done")
    );

    @Rule
    public ContainerRule<GenericContainer<?>> bar = new ContainerRule<>(
        new GenericContainer<>(TINY_IMAGE).withNetwork(network.get()).withCommand("top")
    );

    @Test
    public void testNetworkSupport() throws Exception {
        String response = bar.get().execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
        assertThat(response).as("received response").isEqualTo("yay");
    }
}
