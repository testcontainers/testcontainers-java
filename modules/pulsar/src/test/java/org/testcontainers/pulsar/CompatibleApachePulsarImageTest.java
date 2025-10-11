package org.testcontainers.pulsar;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.DockerImageName;

class CompatibleApachePulsarImageTest extends AbstractPulsar {

    public static String[] params() {
        return new String[] { "apachepulsar/pulsar:3.0.0", "apachepulsar/pulsar-all:3.0.0" };
    }

    @ParameterizedTest
    @MethodSource("params")
    void testUsage(String imageName) throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(DockerImageName.parse(imageName));) {
            pulsar.start();
            final String pulsarBrokerUrl = pulsar.getPulsarBrokerUrl();

            testPulsarFunctionality(pulsarBrokerUrl);
        }
    }

    @ParameterizedTest
    @MethodSource("params")
    void testTransactions(String imageName) throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(DockerImageName.parse(imageName)).withTransactions();) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
                assertTransactionsTopicCreated(pulsarAdmin);
            }
            testTransactionFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }
}
