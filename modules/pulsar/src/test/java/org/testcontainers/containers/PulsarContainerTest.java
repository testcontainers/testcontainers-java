package org.testcontainers.containers;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PulsarContainerTest extends AbstractPulsar {

    private static final DockerImageName PULSAR_IMAGE = DockerImageName.parse("apachepulsar/pulsar:3.0.0");

    @Test
    public void testUsage() throws Exception {
        try (
            // do not use PULSAR_IMAGE to make the doc looks easier
            // constructorWithVersion {
            PulsarContainer pulsar = new PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:3.0.0"));
            // }
        ) {
            pulsar.start();
            // coordinates {
            final String pulsarBrokerUrl = pulsar.getPulsarBrokerUrl();
            final String httpServiceUrl = pulsar.getHttpServiceUrl();
            // }
            testPulsarFunctionality(pulsarBrokerUrl);
        }
    }

    @Test
    public void envVarsUsage() throws Exception {
        try (
            // constructorWithEnv {
            PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE)
                .withEnv("PULSAR_PREFIX_brokerDeduplicationEnabled", "true");
            // }
        ) {
            pulsar.start();
            testPulsarFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    @Test
    public void customClusterName() throws Exception {
        try (
            PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE)
                .withEnv("PULSAR_PREFIX_clusterName", "tc-cluster");
        ) {
            pulsar.start();
            testPulsarFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    @Test
    public void shouldNotEnableFunctionsWorkerByDefault() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE)) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
                assertThatThrownBy(() -> pulsarAdmin.functions().getFunctions("public", "default"))
                    .isInstanceOf(PulsarAdminException.class);
            }
        }
    }

    @Test
    public void shouldWaitForFunctionsWorkerStarted() throws Exception {
        try (
            // constructorWithFunctionsWorker {
            PulsarContainer pulsar = new PulsarContainer(DockerImageName.parse("apachepulsar/pulsar:3.0.0"))
                .withFunctionsWorker();
            // }
        ) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
                assertThat(pulsarAdmin.functions().getFunctions("public", "default")).hasSize(0);
            }
        }
    }

    @Test
    public void testTransactions() throws Exception {
        try (
            // constructorWithTransactions {
            PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE).withTransactions();
            // }
        ) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
                assertTransactionsTopicCreated(pulsarAdmin);
            }
            testTransactionFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    @Test
    public void testTransactionsAndFunctionsWorker() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE).withTransactions().withFunctionsWorker()) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build();) {
                assertTransactionsTopicCreated(pulsarAdmin);
                assertThat(pulsarAdmin.functions().getFunctions("public", "default")).hasSize(0);
            }
            testTransactionFunctionality(pulsar.getPulsarBrokerUrl());
        }
    }

    @Test
    public void testClusterFullyInitialized() throws Exception {
        try (PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE)) {
            pulsar.start();

            try (PulsarAdmin pulsarAdmin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build()) {
                assertThat(pulsarAdmin.clusters().getClusters()).hasSize(1).contains("standalone");
            }
        }
    }

    @Test
    public void testStartupTimeoutIsHonored() {
        try (PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE).withStartupTimeout(Duration.ZERO)) {
            assertThatThrownBy(pulsar::start)
                .hasRootCauseMessage("Precondition failed: timeout must be greater than zero");
        }
    }
}
