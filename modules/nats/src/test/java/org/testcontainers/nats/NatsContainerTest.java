package org.testcontainers.nats;

import io.nats.client.Connection;
import io.nats.client.JetStream;
import io.nats.client.JetStreamManagement;
import io.nats.client.Message;
import io.nats.client.Nats;
import io.nats.client.Options;
import io.nats.client.api.StorageType;
import io.nats.client.api.StreamConfiguration;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class NatsContainerTest {

    private static final DockerImageName NATS_IMAGE = DockerImageName.parse("nats:2.12.1");

    @Test
    void shouldStartNATSContainer() {
        try (NatsContainer natsContainer = new NatsContainer(NATS_IMAGE)) {
            natsContainer.start();

            assertThat(natsContainer.getClientPort()).isNotNull();
            assertThat(natsContainer.getRoutingPort()).isNotNull();
            assertThat(natsContainer.getHttpMonitoringPort()).isNotNull();

            assertThat(natsContainer.getConnectionUrl())
                .isEqualTo(String.format("nats://%s:%d", natsContainer.getHost(), natsContainer.getClientPort()));

            assertThat(natsContainer.getHttpMonitoringUrl())
                .isEqualTo(
                    String.format("http://%s:%d", natsContainer.getHost(), natsContainer.getHttpMonitoringPort())
                );
        }
    }

    @Test
    void shouldPublishAndSubscribeMessages() throws IOException, InterruptedException, TimeoutException {
        try (NatsContainer natsContainer = new NatsContainer(NATS_IMAGE)) {
            natsContainer.start();

            String subject = "test-subject";
            String message = "Hello NATS!";

            Options options = new Options.Builder().server(natsContainer.getConnectionUrl()).build();

            try (Connection nc = Nats.connect(options)) {
                // Subscribe
                io.nats.client.Subscription subscription = nc.subscribe(subject);
                nc.flush(Duration.ofSeconds(1));

                // Publish
                nc.publish(subject, message.getBytes(StandardCharsets.UTF_8));
                nc.flush(Duration.ofSeconds(1));

                // Receive
                Message msg = subscription.nextMessage(Duration.ofSeconds(5));
                assertThat(msg).isNotNull();
                assertThat(new String(msg.getData(), StandardCharsets.UTF_8)).isEqualTo(message);
            }
        }
    }

    @Test
    void shouldSupportJetStream() throws Exception {
        try (NatsContainer natsContainer = new NatsContainer(NATS_IMAGE).withJetStream()) {
            natsContainer.start();

            Options options = new Options.Builder().server(natsContainer.getConnectionUrl()).build();

            try (Connection nc = Nats.connect(options)) {
                JetStreamManagement jsm = nc.jetStreamManagement();

                // Create a stream
                StreamConfiguration streamConfig = StreamConfiguration
                    .builder()
                    .name("test-stream")
                    .subjects("test.>")
                    .storageType(StorageType.Memory)
                    .build();

                jsm.addStream(streamConfig);

                // Get JetStream context
                JetStream js = nc.jetStream();

                // Publish a message
                String subject = "test.foo";
                String message = "JetStream test message";
                js.publish(subject, message.getBytes(StandardCharsets.UTF_8));

                // Subscribe and receive
                io.nats.client.JetStreamSubscription subscription = js.subscribe(subject);
                Message msg = subscription.nextMessage(Duration.ofSeconds(5));

                assertThat(msg).isNotNull();
                assertThat(new String(msg.getData(), StandardCharsets.UTF_8)).isEqualTo(message);

                msg.ack();
            }
        }
    }

    @Test
    void shouldSupportAuthentication() throws IOException, InterruptedException, TimeoutException {
        String username = "testuser";
        String password = "testpassword";

        try (NatsContainer natsContainer = new NatsContainer(NATS_IMAGE).withAuth(username, password)) {
            natsContainer.start();

            Options options = new Options.Builder()
                .server(natsContainer.getConnectionUrl())
                .userInfo(username, password)
                .build();

            try (Connection nc = Nats.connect(options)) {
                assertThat(nc.getStatus()).isEqualTo(Connection.Status.CONNECTED);

                // Test basic pub/sub
                String subject = "auth-test";
                String message = "Authenticated message";

                io.nats.client.Subscription subscription = nc.subscribe(subject);
                nc.flush(Duration.ofSeconds(1));

                nc.publish(subject, message.getBytes(StandardCharsets.UTF_8));
                nc.flush(Duration.ofSeconds(1));

                Message msg = subscription.nextMessage(Duration.ofSeconds(5));
                assertThat(msg).isNotNull();
                assertThat(new String(msg.getData(), StandardCharsets.UTF_8)).isEqualTo(message);
            }
        }
    }

    @Test
    void shouldExposeCorrectPorts() {
        try (NatsContainer natsContainer = new NatsContainer(NATS_IMAGE)) {
            natsContainer.start();

            assertThat(natsContainer.getExposedPorts()).contains(4222, 6222, 8222);
            assertThat(natsContainer.getLivenessCheckPortNumbers())
                .containsExactlyInAnyOrder(
                    natsContainer.getMappedPort(4222),
                    natsContainer.getMappedPort(6222),
                    natsContainer.getMappedPort(8222)
                );
        }
    }
}
