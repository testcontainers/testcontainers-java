package org.testcontainers.containers;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.utility.DockerImageName;

@RunWith(Parameterized.class)
public class CompatibleApachePulsarImageTest extends AbstractPulsar {

    @Parameterized.Parameters(name = "{0}")
    public static String[] params() {
        return new String[] { "apachepulsar/pulsar:3.0.0", "apachepulsar/pulsar-all:3.0.0" };
    }

    @Parameterized.Parameter
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
