package org.testcontainers.containers;

import org.apache.pulsar.client.admin.ListTopicsOptions;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.transaction.Transaction;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PulsarContainerTest {

    public static final String TEST_TOPIC = "test_topic";

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
            PulsarContainer pulsar = new PulsarContainer(PULSAR_IMAGE).withFunctionsWorker();
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

    private void assertTransactionsTopicCreated(PulsarAdmin pulsarAdmin) throws PulsarAdminException {
        final List<String> topics = pulsarAdmin
            .topics()
            .getPartitionedTopicList("pulsar/system", ListTopicsOptions.builder().includeSystemTopic(true).build());
        assertThat(topics).contains("persistent://pulsar/system/transaction_coordinator_assign");
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

    protected void testPulsarFunctionality(String pulsarBrokerUrl) throws Exception {
        try (
            PulsarClient client = PulsarClient.builder().serviceUrl(pulsarBrokerUrl).build();
            Consumer consumer = client.newConsumer().topic(TEST_TOPIC).subscriptionName("test-subs").subscribe();
            Producer<byte[]> producer = client.newProducer().topic(TEST_TOPIC).create()
        ) {
            producer.send("test containers".getBytes());
            CompletableFuture<Message> future = consumer.receiveAsync();
            Message message = future.get(5, TimeUnit.SECONDS);

            assertThat(new String(message.getData())).isEqualTo("test containers");
        }
    }

    protected void testTransactionFunctionality(String pulsarBrokerUrl) throws Exception {
        try (
            PulsarClient client = PulsarClient.builder().serviceUrl(pulsarBrokerUrl).enableTransaction(true).build();
            Consumer<String> consumer = client
                .newConsumer(Schema.STRING)
                .topic("transaction-topic")
                .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                .subscriptionName("test-transaction-sub")
                .subscribe();
            Producer<String> producer = client
                .newProducer(Schema.STRING)
                .sendTimeout(0, TimeUnit.SECONDS)
                .topic("transaction-topic")
                .create()
        ) {
            final Transaction transaction = client.newTransaction().build().get();
            producer.newMessage(transaction).value("first").send();
            transaction.commit();
            Message<String> message = consumer.receive();
            assertThat(message.getValue()).isEqualTo("first");
        }
    }
}
