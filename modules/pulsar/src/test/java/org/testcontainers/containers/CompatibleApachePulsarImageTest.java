package org.testcontainers.containers;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.utility.DockerImageName;

@ParameterizedClass
@MethodSource("params")
public class CompatibleApachePulsarImageTest extends AbstractPulsar {

    public static String[] params() {
        return new String[] { "apachepulsar/pulsar:3.0.0", "apachepulsar/pulsar-all:3.0.0" };
    }

    @Parameter(0)
    public String imageName;

    @Test
    public void testUsage() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(DockerImageName.parse(this.imageName));) {
            pulsar.start();
            final String pulsarBrokerUrl = pulsar.getPulsarBrokerUrl();

            testPulsarFunctionality(pulsarBrokerUrl);
        }
    }

    @Test
    public void testTransactions() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(DockerImageName.parse(this.imageName)).withTransactions();) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
                assertTransactionsTopicCreated(pulsarAdmin);
            }
            testTransactionFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }
}
