package com.example;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Ulimit;
import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishCorrelatingEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author Tomasz Forys
 */
public class SolaceContainerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SolaceContainerTest.class);

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("solace/solace-pubsub-standard");

    private static final String DEFAULT_TAG = "10.2";

    private static final String MESSAGE = "HelloWorld";

    private static final String TOPIC_NAME = "TopicSMF/ActualTopic";

    private static final Topic TOPIC = JCSMPFactory.onlyInstance().createTopic(TOPIC_NAME);

    @Test
    public void testSolaceContainerWithSimpleAuthentication() {
        try (
            SolaceContainer solace = new SolaceContainer(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG))
                .withCredentials("user", "pass")
                .withTopic(TOPIC_NAME, Protocol.SMF)
                .withVpn("test_vpn")
        ) {
            solace.start();
            JCSMPSession session = createSessionWithBasicAuth(solace);
            Assertions.assertThat(session).isNotNull();
            Assertions.assertThat(consumeMessageFromSolace(session)).isEqualTo(MESSAGE);
            session.closeSession();
        }
    }

    @Test
    public void testSolaceContainerWithCertificates() throws URISyntaxException {
        try (
            SolaceContainer solace = new SolaceContainer(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG))
                .withClientCert(
                    MountableFile.forClasspathResource("solace.pem"),
                    MountableFile.forClasspathResource("rootCA.crt")
                )
                .withTopic(TOPIC_NAME, Protocol.SMF_SSL)
        ) {
            solace.start();
            JCSMPSession session = createSessionWithCertificates(solace);
            Assertions.assertThat(session).isNotNull();
            Assertions.assertThat(consumeMessageFromSolace(session)).isEqualTo(MESSAGE);
            session.closeSession();
        }
    }

    private Path getResourceFile(String name) throws URISyntaxException {
        return Paths.get(Thread.currentThread().getContextClassLoader().getResource(name).toURI());
    }

    private static JCSMPSession createSessionWithBasicAuth(SolaceContainer solace) {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solace.getOrigin(Protocol.SMF));
        properties.setProperty(JCSMPProperties.VPN_NAME, solace.getVpn());
        properties.setProperty(JCSMPProperties.USERNAME, solace.getUsername());
        properties.setProperty(JCSMPProperties.PASSWORD, solace.getPassword());
        return createSession(properties);
    }

    private JCSMPSession createSessionWithCertificates(SolaceContainer solace) throws URISyntaxException {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, solace.getOrigin(Protocol.SMF_SSL));
        properties.setProperty(JCSMPProperties.VPN_NAME, solace.getVpn());
        properties.setProperty(JCSMPProperties.USERNAME, solace.getUsername());
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE, true);
        properties.setProperty(JCSMPProperties.SSL_VALIDATE_CERTIFICATE_DATE, true);
        properties.setProperty(
            JCSMPProperties.AUTHENTICATION_SCHEME,
            JCSMPProperties.AUTHENTICATION_SCHEME_CLIENT_CERTIFICATE
        );
        properties.setProperty(JCSMPProperties.SSL_TRUST_STORE, getResourceFile("truststore").toString());
        properties.setProperty(JCSMPProperties.SSL_TRUST_STORE_PASSWORD, "solace");
        properties.setProperty(JCSMPProperties.SSL_KEY_STORE, getResourceFile("client.pfx").toString());
        properties.setProperty(JCSMPProperties.SSL_KEY_STORE_PASSWORD, "solace");
        return createSession(properties);
    }

    private static JCSMPSession createSession(JCSMPProperties properties) {
        try {
            JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();
            return session;
        } catch (Exception e) {
            Assert.fail("Error connecting and setting up session! " + e.getMessage());
            return null;
        }
    }

    private void publishMessageToSolace(JCSMPSession session) throws JCSMPException {
        XMLMessageProducer producer = session.getMessageProducer(
            new JCSMPStreamingPublishCorrelatingEventHandler() {
                @Override
                public void responseReceivedEx(Object o) {
                    LOGGER.info("Producer received response for msg: " + o);
                }

                @Override
                public void handleErrorEx(Object o, JCSMPException e, long l) {
                    LOGGER.error(String.format("Producer received error for msg: %s - %s", o, e));
                }
            }
        );
        TextMessage msg = producer.createTextMessage();
        msg.setText(MESSAGE);
        producer.send(msg, TOPIC);
    }

    private String consumeMessageFromSolace(JCSMPSession session) {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            String[] result = new String[1];
            XMLMessageConsumer cons = session.getMessageConsumer(
                new XMLMessageListener() {
                    @Override
                    public void onReceive(BytesXMLMessage msg) {
                        if (msg instanceof TextMessage textMessage) {
                            String message = textMessage.getText();
                            result[0] = message;
                            LOGGER.info("TextMessage received: " + message);
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onException(JCSMPException e) {
                        LOGGER.error("Exception received: " + e.getMessage());
                        latch.countDown();
                    }
                }
            );
            session.addSubscription(TOPIC);
            cons.start();
            publishMessageToSolace(session);
            Assertions.assertThat(latch.await(10L, TimeUnit.SECONDS)).isTrue();
            return result[0];
        } catch (Exception e) {
            throw new RuntimeException("Cannot receive message from solace", e);
        }
    }

    static class SolaceContainer extends GenericContainer<SolaceContainer> {

        private static final String NEW_LINE = System.getProperty("line.separator");

        private static final String DEFAULT_VPN = "default";

        private static final String DEFAULT_USERNAME = "default";

        private static final String SOLACE_READY_MESSAGE = ".*Running pre-startup checks:.*";

        private static final String SOLACE_ACTIVE_MESSAGE = "Primary Virtual Router is now active";

        private static final String TMP_SCRIPT_LOCATION = "/tmp/script.cli";

        private static final List<String> CONFIG_SOLACE_CLI = Arrays.asList(
            "/usr/sw/loads/currentload/bin/cli",
            "-A",
            "-es",
            "script.cli"
        );

        private static final Long SHM_SIZE = (long) Math.pow(1024, 3);

        private String username = "root";

        private String password = "password";

        private String vpn = DEFAULT_VPN;

        private final List<Pair<String, Protocol>> topicsConfiguration = new ArrayList<>();

        private boolean withClientCert;

        public SolaceContainer(final DockerImageName dockerImageName) {
            super(dockerImageName.toString());
            dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
            this.waitStrategy = Wait.forLogMessage(SOLACE_READY_MESSAGE, 1).withStartupTimeout(Duration.ofSeconds(60));
        }

        @Override
        protected void configure() {
            withCreateContainerCmdModifier(cmd -> {
                cmd
                    .getHostConfig()
                    .withShmSize(SHM_SIZE)
                    .withUlimits(new Ulimit[] { new Ulimit("nofile", 2448L, 6592L) });
            });
            configureSolace();
        }

        private void configureSolace() {
            withCopyToContainer(createConfigurationScript(), TMP_SCRIPT_LOCATION);
        }

        @Override
        protected void containerIsStarted(InspectContainerResponse containerInfo) {
            if (withClientCert) {
                executeCommand(Arrays.asList("cp", "/tmp/solace.pem", "/usr/sw/jail/certs/solace.pem"));
                executeCommand(Arrays.asList("cp", "/tmp/rootCA.crt", "/usr/sw/jail/certs/rootCA.crt"));
            }
            executeCommand(Arrays.asList("cp", TMP_SCRIPT_LOCATION, "/usr/sw/jail/cliscripts/script.cli"));
            waitOnCommandResult(
                Arrays.asList("grep", "-R", SOLACE_ACTIVE_MESSAGE, "/usr/sw/jail/logs/system.log"),
                SOLACE_ACTIVE_MESSAGE
            );
            executeCommand(CONFIG_SOLACE_CLI);
        }

        private Transferable createConfigurationScript() {
            StringBuilder scriptBuilder = new StringBuilder();
            updateConfigScript(scriptBuilder, "enable");
            updateConfigScript(scriptBuilder, "configure");

            // Create VPN if not default
            if (!vpn.equals(DEFAULT_VPN)) {
                updateConfigScript(scriptBuilder, "create message-vpn " + vpn);
                updateConfigScript(scriptBuilder, "no shutdown");
                updateConfigScript(scriptBuilder, "exit");
            }

            // Configure username and password
            if (username.equals(DEFAULT_USERNAME)) {
                throw new RuntimeException("Cannot override password for default client");
            }
            updateConfigScript(scriptBuilder, "create client-username " + username + " message-vpn " + vpn);
            updateConfigScript(scriptBuilder, "password " + password);
            updateConfigScript(scriptBuilder, "no shutdown");
            updateConfigScript(scriptBuilder, "exit");

            if (withClientCert) {
                // Client certificate authority configuration
                updateConfigScript(scriptBuilder, "authentication");
                updateConfigScript(scriptBuilder, "create client-certificate-authority RootCA");
                updateConfigScript(scriptBuilder, "certificate file rootCA.crt");
                updateConfigScript(scriptBuilder, "show client-certificate-authority ca-name *");
                updateConfigScript(scriptBuilder, "end");

                // Server certificates configuration
                updateConfigScript(scriptBuilder, "configure");
                updateConfigScript(scriptBuilder, "ssl");
                updateConfigScript(scriptBuilder, "server-certificate solace.pem");
                updateConfigScript(scriptBuilder, "cipher-suite msg-backbone name AES128-SHA");
                updateConfigScript(scriptBuilder, "exit");

                updateConfigScript(scriptBuilder, "message-vpn " + vpn);
                // Enable client certificate authentication
                updateConfigScript(scriptBuilder, "authentication client-certificate");
                updateConfigScript(scriptBuilder, "allow-api-provided-username");
                updateConfigScript(scriptBuilder, "no shutdown");
                updateConfigScript(scriptBuilder, "end");
            } else {
                // Configure VPN Basic authentication
                updateConfigScript(scriptBuilder, "message-vpn " + vpn);
                updateConfigScript(scriptBuilder, "authentication basic auth-type internal");
                updateConfigScript(scriptBuilder, "no shutdown");
                updateConfigScript(scriptBuilder, "end");
            }

            if (!topicsConfiguration.isEmpty()) {
                // Enable services
                updateConfigScript(scriptBuilder, "configure");
                // Configure default ACL
                updateConfigScript(scriptBuilder, "acl-profile default message-vpn " + vpn);
                // Configure default action to disallow
                updateConfigScript(scriptBuilder, "subscribe-topic default-action disallow");
                updateConfigScript(scriptBuilder, "publish-topic default-action disallow");
                updateConfigScript(scriptBuilder, "exit");

                updateConfigScript(scriptBuilder, "message-vpn " + vpn);
                updateConfigScript(scriptBuilder, "service");
                for (Pair<String, Protocol> topicConfig : topicsConfiguration) {
                    Protocol protocol = topicConfig.getValue();
                    updateConfigScript(scriptBuilder, protocol.getService());
                    if (protocol.isSupportSSL()) {
                        if (withClientCert) {
                            updateConfigScript(scriptBuilder, "ssl");
                        } else {
                            updateConfigScript(scriptBuilder, "plain-text");
                        }
                    }
                    updateConfigScript(scriptBuilder, "no shutdown");
                    updateConfigScript(scriptBuilder, "end");
                    // Add publish/subscribe topic exceptions
                    updateConfigScript(scriptBuilder, "configure");
                    updateConfigScript(scriptBuilder, "acl-profile default message-vpn " + vpn);
                    updateConfigScript(
                        scriptBuilder,
                        "publish-topic exceptions " +
                        topicConfig.getValue().getService() +
                        " list " +
                        topicConfig.getKey()
                    );
                    updateConfigScript(
                        scriptBuilder,
                        "subscribe-topic exceptions " +
                        topicConfig.getValue().getService() +
                        " list " +
                        topicConfig.getKey()
                    );
                    updateConfigScript(scriptBuilder, "end");
                }
            }
            return Transferable.of(scriptBuilder.toString());
        }

        private void executeCommand(List<String> command) {
            try {
                ExecResult execResult = execInContainer(command.toArray(new String[0]));
                if (execResult.getExitCode() != 0) {
                    logger().error("Could not execute command {}: {}", command, execResult.getStderr());
                }
            } catch (IOException | InterruptedException e) {
                logger().error("Could not execute command {}: {}", command, e.getMessage());
            }
        }

        private void updateConfigScript(StringBuilder scriptBuilder, String command) {
            scriptBuilder.append(command).append(NEW_LINE);
        }

        private void waitOnCommandResult(List<String> command, String waitingFor) {
            Awaitility
                .await()
                .pollInterval(Duration.ofMillis(500))
                .timeout(Duration.ofSeconds(30))
                .until(() -> {
                    try {
                        return execInContainer(command.toArray(new String[0])).getStdout().contains(waitingFor);
                    } catch (IOException | InterruptedException e) {
                        logger().error("Could not execute command {}: {}", command, e.getMessage());
                        return true;
                    }
                });
        }

        public SolaceContainer withCredentials(final String username, final String password) {
            this.username = username;
            this.password = password;
            return this;
        }

        public SolaceContainer withTopic(String topic, Protocol protocol) {
            topicsConfiguration.add(Pair.of(topic, protocol));
            addExposedPort(protocol.getPort());
            return this;
        }

        public SolaceContainer withVpn(String vpn) {
            this.vpn = vpn;
            return this;
        }

        public SolaceContainer withClientCert(final MountableFile certFile, final MountableFile caFile) {
            this.withClientCert = true;
            return withCopyFileToContainer(certFile, "/tmp/solace.pem")
                .withCopyFileToContainer(caFile, "/tmp/rootCA.crt");
        }

        public String getVpn() {
            return vpn;
        }

        public String getOrigin(Protocol port) {
            return String.format("%s://%s:%s", port.getProtocol(), getHost(), getMappedPort(port.getPort()));
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }
    }

    public enum Protocol {
        AMQP("amqp", 5672, "amqp", false),
        MQTT("mqtt", 1883, "tcp", false),
        REST("rest", 9000, "http", false),
        SEMP("semp", 8080, "http", false),
        SMF("smf", 55555, "tcp", true),
        SMF_SSL("smf", 55443, "tcps", true);

        private final String service;
        private final Integer port;
        private final String protocol;
        private final boolean supportSSL;

        Protocol(String service, Integer port, String protocol, boolean supportSSL) {
            this.service = service;
            this.port = port;
            this.protocol = protocol;
            this.supportSSL = supportSSL;
        }

        public Integer getPort() {
            return port;
        }

        private String getProtocol() {
            return protocol;
        }

        public String getService() {
            return service;
        }

        public boolean isSupportSSL() {
            return supportSSL;
        }
    }
}
