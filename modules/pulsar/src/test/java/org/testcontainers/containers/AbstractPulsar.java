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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractPulsar {

    public static final String TEST_TOPIC = "test_topic";

    protected void testPulsarFunctionality(String pulsarBrokerUrl) throws Exception {
        try (
            PulsarClient client = PulsarClient.builder().serviceUrl(pulsarBrokerUrl).build();
            Consumer<byte[]> consumer = client
                .newConsumer()
                .topic(TEST_TOPIC)
                .subscriptionName("test-subs")
                .subscribe();
            Producer<byte[]> producer = client.newProducer().topic(TEST_TOPIC).create()
        ) {
            producer.send("test containers".getBytes());
            CompletableFuture<Message<byte[]>> future = consumer.receiveAsync();
            Message<byte[]> message = future.get(5, TimeUnit.SECONDS);

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

    protected void assertTransactionsTopicCreated(PulsarAdmin pulsarAdmin) throws PulsarAdminException {
        final List<String> topics = pulsarAdmin
            .topics()
            .getPartitionedTopicList("pulsar/system", ListTopicsOptions.builder().includeSystemTopic(true).build());
        assertThat(topics).contains("persistent://pulsar/system/transaction_coordinator_assign");
    }
}
