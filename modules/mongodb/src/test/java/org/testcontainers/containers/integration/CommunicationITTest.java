package org.testcontainers.containers.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDbContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.core.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests both:
 * <ul>
 * <li>the communication between 2 MongoDbContainers</li>
 * <li>the accessibility of these MongoDbContainers</li>
 * </ul>
 * from a host.
 * <p>
 * Note that s256/mongodb-net-utils is an official MongoDB Docker file with the nmap package installed.
 * Its tag corresponds to an official MongoDB Docker file tag.
 *
 * @see <a href="https://hub.docker.com/r/s256/mongodb-net-utils/dockerfile">dockerfile on DockerHub</a>
 * @see <a href="https://github.com/silaev/mongodb-net-utils">mongodb-net-utils on GitHub</a>
 */
@IntegrationTest
@Slf4j
class CommunicationITTest extends BaseInitializationITTest {
    private final Network network = Network.newNetwork();
    private final MongoDbContainer mongoDbContainer1 =
        new MongoDbContainer("s256/mongodb-net-utils:4.0.10")
            .withNetworkAliases("mongoDbContainer1")
            .withNetwork(network);
    private final MongoDbContainer mongoDbContainer2 =
        new MongoDbContainer("s256/mongodb-net-utils:4.0.10")
            .withNetworkAliases("mongoDbContainer2")
            .withNetwork(network);

    @BeforeEach
    void setUp() {
        mongoDbContainer1.start();
        mongoDbContainer2.start();
    }

    @AfterEach
    void tearDown() {
        mongoDbContainer1.stop();
        mongoDbContainer2.stop();
    }

    @Test
    void shouldTestCommunication() {
        testCommunicationBetweenNodes();
        testAccessibilityFromHost();
    }

    @SneakyThrows
    private void testCommunicationBetweenNodes() {
        //GIVEN
        final String successString = "Host is up";

        //WHEN
        final String stdout1 =
            mongoDbContainer1.execInContainer("nmap", "mongoDbContainer2")
                .getStdout();
        log.debug("Pinging mongoDbContainer2 from mongoDbContainer1: \n{}", stdout1);

        final String stdout2 =
            mongoDbContainer2.execInContainer("nmap", "mongoDbContainer1")
                .getStdout();
        log.debug("Pinging mongoDbContainer1 from mongoDbContainer2: \n{}", stdout2);

        final String stdout3 =
            mongoDbContainer2.execInContainer("nmap", "mongoDbContainer15")
                .getStdout();

        //THEN
        assertTrue(stdout1.contains(successString));
        assertTrue(stdout2.contains(successString));
        assertFalse(stdout3.contains(successString));
    }

    private void testAccessibilityFromHost() {
        super.shouldTestRsStatus(mongoDbContainer1);
        super.shouldTestRsStatus(mongoDbContainer2);
    }
}
