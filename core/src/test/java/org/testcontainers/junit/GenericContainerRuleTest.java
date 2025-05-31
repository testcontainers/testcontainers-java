package org.testcontainers.junit;

import com.github.dockerjava.api.model.HostConfig;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Uninterruptibles;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.apache.commons.io.FileUtils;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.TestImages;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.Base58;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests for GenericContainerRules
 */
public class GenericContainerRuleTest {

    private static final int REDIS_PORT = 6379;

    private static final String RABBIQMQ_TEST_EXCHANGE = "TestExchange";

    private static final String RABBITMQ_TEST_ROUTING_KEY = "TestRoutingKey";

    private static final String RABBITMQ_TEST_MESSAGE = "Hello world";

    private static final int RABBITMQ_PORT = 5672;

    private static final int MONGO_PORT = 27017;

    /*
     * Test data setup
     */
    @BeforeClass
    public static void setupContent() throws FileNotFoundException {
        File contentFolder = new File(System.getProperty("user.home") + "/.tmp-test-container");
        contentFolder.mkdir();
        writeStringToFile(contentFolder, "file", "Hello world!");
    }

    /**
     * Redis
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> redis = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.REDIS_IMAGE).withExposedPorts(REDIS_PORT)
    );

    /**
     * RabbitMQ
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> rabbitMq = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.RABBITMQ_IMAGE).withExposedPorts(RABBITMQ_PORT)
    );

    /**
     * MongoDB
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> mongo = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.MONGODB_IMAGE).withExposedPorts(MONGO_PORT)
    );

    /**
     * Pass an environment variable to the container, then run a shell script that exposes the variable in a quick and
     * dirty way for testing.
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> alpineEnvVar = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts(80)
            .withEnv("MAGIC_NUMBER", "4")
            .withEnv("MAGIC_NUMBER", oldValue -> oldValue.orElse("") + "2")
            .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done")
    );

    /**
     * Pass environment variables to the container, then run a shell script that exposes the variables in a quick and
     * dirty way for testing.
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> alpineEnvVarFromMap = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts(80)
            .withEnv(ImmutableMap.of("FIRST", "42", "SECOND", "50"))
            .withCommand("/bin/sh", "-c", "while true; do echo \"$FIRST and $SECOND\" | nc -l -p 80; done")
    );

    /**
     * Map a file on the classpath to a file in the container, and then expose the content for testing.
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> alpineClasspathResource = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts(80)
            .withClasspathResourceMapping("mappable-resource/test-resource.txt", "/content.txt", BindMode.READ_ONLY)
            .withCommand("/bin/sh", "-c", "while true; do cat /content.txt | nc -l -p 80; done")
    );

    /**
     * Map a file on the classpath to a file in the container, and then expose the content for testing.
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> alpineClasspathResourceSelinux = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts(80)
            .withClasspathResourceMapping(
                "mappable-resource/test-resource.txt",
                "/content.txt",
                BindMode.READ_WRITE,
                SelinuxContext.SHARED
            )
            .withCommand("/bin/sh", "-c", "while true; do cat /content.txt | nc -l -p 80; done")
    );

    /**
     * Create a container with an extra host entry and expose the content of /etc/hosts for testing.
     */
    @ClassRule
    public static TestcontainersRule<GenericContainer<?>> alpineExtrahost = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts(80)
            .withExtraHost("somehost", "192.168.1.10")
            .withCommand("/bin/sh", "-c", "while true; do cat /etc/hosts | nc -l -p 80; done")
    );

    @Test
    public void testIsRunning() {
        try (GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("top")) {
            assertThat(container.isRunning()).as("Container is not started and not running").isFalse();
            container.start();
            assertThat(container.isRunning()).as("Container is started and running").isTrue();
        }
    }

    @Test
    public void withTmpFsTest() throws Exception {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("top")
                .withTmpFs(Collections.singletonMap("/testtmpfs", "rw"))
        ) {
            container.start();
            // check file doesn't exist
            String path = "/testtmpfs/test.file";
            Container.ExecResult execResult = container.execInContainer("ls", path);
            assertThat(execResult.getStderr())
                .as("tmpfs inside container works fine")
                .isEqualTo("ls: /testtmpfs/test.file: No such file or directory\n");
            // touch && check file does exist
            container.execInContainer("touch", path);
            execResult = container.execInContainer("ls", path);
            assertThat(execResult.getStdout()).as("tmpfs inside container works fine").isEqualTo(path + "\n");
        }
    }

    @Test
    public void simpleRabbitMqTest() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMq.get().getHost());
        factory.setPort(rabbitMq.get().getMappedPort(RABBITMQ_PORT));
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();
        channel.exchangeDeclare(RABBIQMQ_TEST_EXCHANGE, "direct", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, RABBIQMQ_TEST_EXCHANGE, RABBITMQ_TEST_ROUTING_KEY);

        // Set up a consumer on the queue
        final boolean[] messageWasReceived = new boolean[1];
        channel.basicConsume(
            queueName,
            false,
            new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body
                ) throws IOException {
                    messageWasReceived[0] = Arrays.equals(body, RABBITMQ_TEST_MESSAGE.getBytes());
                }
            }
        );

        // post a message
        channel.basicPublish(RABBIQMQ_TEST_EXCHANGE, RABBITMQ_TEST_ROUTING_KEY, null, RABBITMQ_TEST_MESSAGE.getBytes());

        // check the message was received
        assertThat(
            Unreliables.retryUntilSuccess(
                5,
                TimeUnit.SECONDS,
                () -> {
                    if (!messageWasReceived[0]) {
                        throw new IllegalStateException("Message not received yet");
                    }
                    return true;
                }
            )
        )
            .as("The message was received")
            .isTrue();
    }

    @Test
    public void simpleMongoDbTest() {
        MongoClient mongoClient = new MongoClient(mongo.get().getHost(), mongo.get().getMappedPort(MONGO_PORT));
        MongoDatabase database = mongoClient.getDatabase("test");
        MongoCollection<Document> collection = database.getCollection("testCollection");

        Document doc = new Document("name", "foo").append("value", 1);
        collection.insertOne(doc);

        Document doc2 = collection.find(new Document("name", "foo")).first();
        assertThat(doc2.get("value")).as("A record can be inserted into and retrieved from MongoDB").isEqualTo(1);
    }

    @Test
    public void environmentAndCustomCommandTest() throws IOException {
        String line = getReaderForContainerPort80(alpineEnvVar.get()).readLine();

        assertThat(line).as("An environment variable can be passed into a command").isEqualTo("42");
    }

    @Test
    public void environmentFromMapTest() throws IOException {
        String line = getReaderForContainerPort80(alpineEnvVarFromMap.get()).readLine();

        assertThat(line).as("Environment variables can be passed into a command from a map").isEqualTo("42 and 50");
    }

    @Test
    public void customLabelTest() {
        try (
            final GenericContainer alpineCustomLabel = new GenericContainer<>(TestImages.ALPINE_IMAGE)
                .withLabel("our.custom", "label")
                .withCommand("top")
                .withCreateContainerCmdModifier(cmd -> cmd.getLabels().put("scope", "local"))
        ) {
            alpineCustomLabel.start();

            Map<String, String> labels = alpineCustomLabel.getCurrentContainerInfo().getConfig().getLabels();
            assertThat(labels).as("org.testcontainers label is present").containsKey("org.testcontainers");
            assertThat(labels).as("org.testcontainers.lang label is present").containsKey("org.testcontainers.lang");
            assertThat(labels)
                .as("org.testcontainers.lang label is present")
                .containsEntry("org.testcontainers.lang", "java");
            assertThat(labels)
                .as("org.testcontainers.version label is present")
                .containsKey("org.testcontainers.version");
            assertThat(labels).as("our.custom label is present").containsKey("our.custom");
            assertThat(labels).as("our.custom label value is label").containsEntry("our.custom", "label");
            assertThat(labels)
                .as("project label value is testcontainers-java")
                .containsEntry("project", "testcontainers-java");
            assertThat(labels).as("scope label value is local").containsEntry("scope", "local");
        }
    }

    @Test
    public void exceptionThrownWhenTryingToOverrideTestcontainersLabels() {
        assertThat(
            catchThrowable(() -> {
                new GenericContainer<>(TestImages.ALPINE_IMAGE).withLabel("org.testcontainers.foo", "false");
            })
        )
            .as("When trying to overwrite an 'org.testcontainers' label, withLabel() throws an exception")
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void customClasspathResourceMappingTest() throws IOException {
        // Note: This functionality doesn't work if you are running your build inside a Docker container;
        // in that case this test will fail.
        String line = getReaderForContainerPort80(alpineClasspathResource.get()).readLine();

        assertThat(line)
            .as("Resource on the classpath can be mapped using calls to withClasspathResourceMapping")
            .isEqualTo("FOOBAR");
    }

    @Test
    public void customClasspathResourceMappingWithSelinuxTest() throws IOException {
        String line = getReaderForContainerPort80(alpineClasspathResourceSelinux.get()).readLine();
        assertThat(line)
            .as("Resource on the classpath can be mapped using calls to withClasspathResourceMappingSelinux")
            .isEqualTo("FOOBAR");
    }

    @Test
    public void exceptionThrownWhenMappedPortNotFound() {
        assertThat(catchThrowable(() -> redis.get().getMappedPort(666)))
            .as("When the requested port is not mapped, getMappedPort() throws an exception")
            .isInstanceOf(IllegalArgumentException.class);
    }

    protected static void writeStringToFile(File contentFolder, String filename, String string)
        throws FileNotFoundException {
        File file = new File(contentFolder, filename);

        PrintStream printStream = new PrintStream(new FileOutputStream(file));
        printStream.println(string);
        printStream.close();
    }

    @Test
    @Ignore //TODO investigate intermittent failures
    public void failFastWhenContainerHaltsImmediately() {
        long startingTimeNano = System.nanoTime();
        final GenericContainer failsImmediately = new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withCommand("/bin/sh", "-c", "return false")
            .withMinimumRunningDuration(Duration.ofMillis(100));

        try {
            assertThat(catchThrowable(failsImmediately::start))
                .as("When we start a container that halts immediately, an exception is thrown")
                .isInstanceOf(RetryCountExceededException.class);

            // Check how long it took, to verify that we ARE bailing out early.
            // Want to strike a balance here; too short and this test will fail intermittently
            // on slow systems and/or due to GC variation, too long, and we won't properly test
            // what we're intending to test.
            int allowedSecondsToFailure = GenericContainer.CONTAINER_RUNNING_TIMEOUT_SEC / 2;
            long completedTimeNano = System.nanoTime();
            assertThat(completedTimeNano - startingTimeNano < TimeUnit.SECONDS.toNanos(allowedSecondsToFailure))
                .as("container should not take long to start up")
                .isTrue();
        } finally {
            failsImmediately.stop();
        }
    }

    @Test
    public void extraHostTest() throws IOException {
        BufferedReader br = getReaderForContainerPort80(alpineExtrahost.get());

        // read hosts file from container
        StringBuffer hosts = new StringBuffer();
        String line = br.readLine();
        while (line != null) {
            hosts.append(line);
            hosts.append("\n");
            line = br.readLine();
        }

        Matcher matcher = Pattern.compile("^192.168.1.10\\s.*somehost", Pattern.MULTILINE).matcher(hosts.toString());
        assertThat(matcher.find()).as("The hosts file of container contains extra host").isTrue();
    }

    @Test
    public void createContainerCmdHookTest() {
        // Use random name to avoid the conflicts between the tests
        String randomName = Base58.randomString(5);
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withCommand("redis-server", "--help")
                .withCreateContainerCmdModifier(cmd -> cmd.withName("overrideMe"))
                // Preserves the order
                .withCreateContainerCmdModifier(cmd -> cmd.withName(randomName))
                // Allows to override pre-configured values by GenericContainer
                .withCreateContainerCmdModifier(cmd -> cmd.withCmd("redis-server", "--port", "6379"))
        ) {
            container.start();

            assertThat(container.getContainerInfo().getName()).as("Name is configured").isEqualTo("/" + randomName);
            assertThat(Arrays.toString(container.getContainerInfo().getConfig().getCmd()))
                .as("Command is configured")
                .isEqualTo("[redis-server, --port, 6379]");
        }
    }

    private BufferedReader getReaderForContainerPort80(GenericContainer container) {
        return Unreliables.retryUntilSuccess(
            10,
            TimeUnit.SECONDS,
            () -> {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

                Socket socket = new Socket(container.getHost(), container.getFirstMappedPort());
                return new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
        );
    }

    @Test
    public void addExposedPortAfterWithExposedPortsTest() {
        redis.get().addExposedPort(8987);
        assertThat(redis.get().getExposedPorts()).as("Both ports should be exposed").hasSize(2);
        assertThat(redis.get().getExposedPorts()).as("withExposedPort should be exposed").contains(REDIS_PORT);
        assertThat(redis.get().getExposedPorts()).as("addExposedPort should be exposed").contains(8987);
    }

    @Test
    public void addingExposedPortTwiceShouldNotFail() {
        redis.get().addExposedPort(8987);
        redis.get().addExposedPort(8987);
        assertThat(redis.get().getExposedPorts()).as("Both ports should be exposed").hasSize(2); // 2 ports = de-duplicated port 8897 and original port 6379
        assertThat(redis.get().getExposedPorts()).as("withExposedPort should be exposed").contains(REDIS_PORT);
        assertThat(redis.get().getExposedPorts()).as("addExposedPort should be exposed").contains(8987);
    }

    @Test
    public void sharedMemorySetTest() {
        try (
            GenericContainer containerWithSharedMemory = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withSharedMemorySize(42L * FileUtils.ONE_MB)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            containerWithSharedMemory.start();

            HostConfig hostConfig = containerWithSharedMemory.getContainerInfo().getHostConfig();
            assertThat(hostConfig.getShmSize())
                .as("Shared memory not set on container")
                .isEqualTo(42L * FileUtils.ONE_MB);
        }
    }
}
