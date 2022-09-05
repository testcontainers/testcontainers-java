package org.testcontainers.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class NetworkRunnerTest {

    private static DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.16");

    @Container
    public Network network = Network.newNetwork();

    @Container
    public GenericContainer<?> foo = new GenericContainer<>(TINY_IMAGE)
        .withNetwork(network)
        .withNetworkAliases("foo")
        .withCommand("/bin/sh", "-c", "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done");

    @Container
    public GenericContainer<?> bar = new GenericContainer<>(TINY_IMAGE).withNetwork(network).withCommand("top");

    @Test
    public void testNetworkSupport() throws Exception {
        String response = bar.execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
        assertThat(response).as("received response").isEqualTo("yay");
    }
}
