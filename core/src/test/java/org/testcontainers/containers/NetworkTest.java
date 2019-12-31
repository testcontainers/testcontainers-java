package org.testcontainers.containers;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.testcontainers.DockerClientFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.testcontainers.containers.Network.newNetwork;

@RunWith(Enclosed.class)
public class NetworkTest {

    public static class WithRules {

        @Rule
        public Network network = newNetwork();

        @Rule
        public GenericContainer foo = new GenericContainer()
            .withNetwork(network)
            .withNetworkAliases("foo")
            .withCommand("/bin/sh", "-c", "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done");

        @Rule
        public GenericContainer bar = new GenericContainer()
            .withNetwork(network)
            .withCommand("top");

        @Test
        public void testNetworkSupport() throws Exception {
            String response = bar.execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
            assertEquals("yay", response);
        }
    }

    public static class WithoutRules {

        @Test
        public void testNetworkSupport() throws Exception {
            try (
                Network network = Network.newNetwork();

                GenericContainer foo = new GenericContainer()
                    .withNetwork(network)
                    .withNetworkAliases("foo")
                    .withCommand("/bin/sh", "-c", "while true ; do printf 'HTTP/1.1 200 OK\\n\\nyay' | nc -l -p 8080; done");

                GenericContainer bar = new GenericContainer()
                    .withNetwork(network)
                    .withCommand("top")
            ) {
                foo.start();
                bar.start();

                String response = bar.execInContainer("wget", "-O", "-", "http://foo:8080").getStdout();
                assertEquals("yay", response);
            }
        }

        @Test
        public void testBuilder() {
            try (
                Network network = Network.builder()
                    .driver("macvlan")
                    .build();
            ) {
                String id = network.getId();
                assertEquals(
                    "macvlan",
                    DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(id).exec().getDriver()
                );
            }
        }

        @Test
        public void testModifiers() {
            try (
                Network network = Network.builder()
                    .createNetworkCmdModifier(cmd -> cmd.withDriver("macvlan"))
                    .build();
            ) {
                String id = network.getId();
                assertEquals(
                    "macvlan",
                    DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(id).exec().getDriver()
                );
            }
        }

        @Test
        public void testReusability() {
            try (Network network = Network.newNetwork()) {
                String firstId = network.getId();
                assertNotNull(
                    DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(firstId).exec()
                );

                network.close();

                assertNotEquals(
                    firstId,
                    DockerClientFactory.instance().client().inspectNetworkCmd().withNetworkId(network.getId()).exec().getId()
                );
            }
        }
    }
}

