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
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.TestEnvironment;

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
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.ALPINE_IMAGE;
import static org.testcontainers.TestImages.MONGODB_IMAGE;
import static org.testcontainers.TestImages.RABBITMQ_IMAGE;
import static org.testcontainers.TestImages.REDIS_IMAGE;
import static org.testcontainers.TestImages.TINY_IMAGE;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.BindMode.READ_WRITE;
import static org.testcontainers.containers.SelinuxContext.SHARED;

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
    public static GenericContainer<?> redis = new GenericContainer<>(REDIS_IMAGE)
            .withExposedPorts(REDIS_PORT);

    /**
     * RabbitMQ
     */
    @ClassRule
    public static GenericContainer<?> rabbitMq = new GenericContainer<>(RABBITMQ_IMAGE)
            .withExposedPorts(RABBITMQ_PORT);
    /**
     * MongoDB
     */
    @ClassRule
    public static GenericContainer<?> mongo = new GenericContainer<>(MONGODB_IMAGE)
            .withExposedPorts(MONGO_PORT);
    /**
     * Pass an environment variable to the container, then run a shell script that exposes the variable in a quick and
     * dirty way for testing.
     */
    @ClassRule
    public static GenericContainer<?> alpineEnvVar = new GenericContainer<>(ALPINE_IMAGE)
            .withExposedPorts(80)
            .withEnv("MAGIC_NUMBER", "4")
            .withEnv("MAGIC_NUMBER", oldValue -> oldValue.orElse("") + "2")
            .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");

    /**
     * Pass environment variables to the container, then run a shell script that exposes the variables in a quick and
     * dirty way for testing.
     */
    @ClassRule
    public static GenericContainer<?> alpineEnvVarFromMap = new GenericContainer<>(ALPINE_IMAGE)
            .withExposedPorts(80)
            .withEnv(ImmutableMap.of(
                    "FIRST", "42",
                    "SECOND", "50"
            ))
            .withCommand("/bin/sh", "-c", "while true; do echo \"$FIRST and $SECOND\" | nc -l -p 80; done");

    /**
     * Map a file on the classpath to a file in the container, and then expose the content for testing.
     */
    @ClassRule
    public static GenericContainer<?> alpineClasspathResource = new GenericContainer<>(ALPINE_IMAGE)
            .withExposedPorts(80)
            .withClasspathResourceMapping("mappable-resource/test-resource.txt", "/content.txt", READ_ONLY)
            .withCommand("/bin/sh", "-c", "while true; do cat /content.txt | nc -l -p 80; done");

    /**
     * Map a file on the classpath to a file in the container, and then expose the content for testing.
     */
    @ClassRule
    public static GenericContainer<?> alpineClasspathResourceSelinux = new GenericContainer<>(ALPINE_IMAGE)
            .withExposedPorts(80)
            .withClasspathResourceMapping("mappable-resource/test-resource.txt", "/content.txt", READ_WRITE, SHARED)
            .withCommand("/bin/sh", "-c", "while true; do cat /content.txt | nc -l -p 80; done");

    /**
     * Create a container with an extra host entry and expose the content of /etc/hosts for testing.
     */
    @ClassRule
    public static GenericContainer<?> alpineExtrahost = new GenericContainer<>(ALPINE_IMAGE)
            .withExposedPorts(80)
            .withExtraHost("somehost", "192.168.1.10")
            .withCommand("/bin/sh", "-c", "while true; do cat /etc/hosts | nc -l -p 80; done");

    @Test
    public void testIsRunning() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
            .withCommand("top")) {
            assertFalse("Container is not started and not running", container.isRunning());
            container.start();
            assertTrue("Container is started and running", container.isRunning());
        }
    }

    @Test
    public void withTmpFsTest() throws Exception {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withCommand("top")
                .withTmpFs(singletonMap("/testtmpfs", "rw"))
        ) {
            container.start();
            // check file doesn't exist
            String path = "/testtmpfs/test.file";
            Container.ExecResult execResult = container.execInContainer("ls", path);
            assertEquals("tmpfs inside container works fine", execResult.getStderr(),
                "ls: /testtmpfs/test.file: No such file or directory\n");
            // touch && check file does exist
            container.execInContainer("touch", path);
            execResult = container.execInContainer("ls", path);
            assertEquals("tmpfs inside container works fine", execResult.getStdout(), path + "\n");
        }
    }


    @Test
    public void simpleRabbitMqTest() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMq.getHost());
        factory.setPort(rabbitMq.getMappedPort(RABBITMQ_PORT));
        Connection connection = factory.newConnection();

        Channel channel = connection.createChannel();
        channel.exchangeDeclare(RABBIQMQ_TEST_EXCHANGE, "direct", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, RABBIQMQ_TEST_EXCHANGE, RABBITMQ_TEST_ROUTING_KEY);

        // Set up a consumer on the queue
        final boolean[] messageWasReceived = new boolean[1];
        channel.basicConsume(queueName, false, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                messageWasReceived[0] = Arrays.equals(body, RABBITMQ_TEST_MESSAGE.getBytes());
            }
        });

        // post a message
        channel.basicPublish(RABBIQMQ_TEST_EXCHANGE, RABBITMQ_TEST_ROUTING_KEY, null, RABBITMQ_TEST_MESSAGE.getBytes());

        // check the message was received
        assertTrue("The message was received", Unreliables.retryUntilSuccess(5, TimeUnit.SECONDS, () -> {
            if (!messageWasReceived[0]) {
                throw new IllegalStateException("Message not received yet");
            }
            return true;
        }));
    }

    @Test
    public void simpleMongoDbTest() {
        MongoClient mongoClient = new MongoClient(mongo.getHost(), mongo.getMappedPort(MONGO_PORT));
        MongoDatabase database = mongoClient.getDatabase("test");
        MongoCollection<Document> collection = database.getCollection("testCollection");

        Document doc = new Document("name", "foo")
                .append("value", 1);
        collection.insertOne(doc);

        Document doc2 = collection.find(new Document("name", "foo")).first();
        assertEquals("A record can be inserted into and retrieved from MongoDB", 1, doc2.get("value"));
    }

    @Test
    public void environmentAndCustomCommandTest() throws IOException {
        String line = getReaderForContainerPort80(alpineEnvVar).readLine();

        assertEquals("An environment variable can be passed into a command", "42", line);
    }

    @Test
    public void environmentFromMapTest() throws IOException {
        String line = getReaderForContainerPort80(alpineEnvVarFromMap).readLine();

        assertEquals("Environment variables can be passed into a command from a map", "42 and 50", line);
    }

    @Test
    public void customLabelTest() {
        try (final GenericContainer alpineCustomLabel = new GenericContainer<>(ALPINE_IMAGE)
            .withLabel("our.custom", "label")
            .withCommand("top")) {

            alpineCustomLabel.start();

            Map<String, String> labels = alpineCustomLabel.getCurrentContainerInfo().getConfig().getLabels();
            assertTrue("org.testcontainers label is present", labels.containsKey("org.testcontainers"));
            assertTrue("our.custom label is present", labels.containsKey("our.custom"));
            assertEquals("our.custom label value is label", labels.get("our.custom"), "label");
        }
    }

    @Test
    public void exceptionThrownWhenTryingToOverrideTestcontainersLabels() {
        assertThrows("When trying to overwrite an 'org.testcontainers' label, withLabel() throws an exception",
            IllegalArgumentException.class,
            () -> {
                new GenericContainer<>(ALPINE_IMAGE)
                    .withLabel("org.testcontainers.foo", "false");
            }
        );
    }

    @Test
    public void customClasspathResourceMappingTest() throws IOException {
        // Note: This functionality doesn't work if you are running your build inside a Docker container;
        // in that case this test will fail.
        String line = getReaderForContainerPort80(alpineClasspathResource).readLine();

        assertEquals("Resource on the classpath can be mapped using calls to withClasspathResourceMapping", "FOOBAR", line);
    }

    @Test
    public void customClasspathResourceMappingWithSelinuxTest() throws IOException {
        String line = getReaderForContainerPort80(alpineClasspathResourceSelinux).readLine();
        assertEquals("Resource on the classpath can be mapped using calls to withClasspathResourceMappingSelinux", "FOOBAR", line);
    }

    @Test
    public void exceptionThrownWhenMappedPortNotFound() {
        assertThrows("When the requested port is not mapped, getMappedPort() throws an exception",
                IllegalArgumentException.class,
                () -> {
                    return redis.getMappedPort(666);
                });
    }

    protected static void writeStringToFile(File contentFolder, String filename, String string) throws FileNotFoundException {
        File file = new File(contentFolder, filename);

        PrintStream printStream = new PrintStream(new FileOutputStream(file));
        printStream.println(string);
        printStream.close();
    }

    @Test @Ignore //TODO investigate intermittent failures
    public void failFastWhenContainerHaltsImmediately() {

        long startingTimeMs = System.currentTimeMillis();
        final GenericContainer failsImmediately = new GenericContainer<>(ALPINE_IMAGE)
              .withCommand("/bin/sh", "-c", "return false")
              .withMinimumRunningDuration(Duration.ofMillis(100));

        try {
            assertThrows(
                  "When we start a container that halts immediately, an exception is thrown",
                  RetryCountExceededException.class,
                  () -> {
                      failsImmediately.start();
                      return null;
                  });

            // Check how long it took, to verify that we ARE bailing out early.
            // Want to strike a balance here; too short and this test will fail intermittently
            // on slow systems and/or due to GC variation, too long and we won't properly test
            // what we're intending to test.
            int allowedSecondsToFailure =
                GenericContainer.CONTAINER_RUNNING_TIMEOUT_SEC / 2;
            long completedTimeMs = System.currentTimeMillis();
            assertTrue("container should not take long to start up",
                  completedTimeMs - startingTimeMs < 1000L * allowedSecondsToFailure);
        } finally {
            failsImmediately.stop();
        }
    }

    @Test
    public void testExecInContainer() throws Exception {

        // The older "lxc" execution driver doesn't support "exec". At the time of writing (2016/03/29),
        // that's the case for CircleCI.
        // Once they resolve the issue, this clause can be removed.
        Assume.assumeTrue(TestEnvironment.dockerExecutionDriverSupportsExec());

        final GenericContainer.ExecResult result = redis.execInContainer("redis-cli", "role");
        assertTrue("Output for \"redis-cli role\" command should start with \"master\"", result.getStdout().startsWith("master"));
        assertEquals("Stderr for \"redis-cli role\" command should be empty", "", result.getStderr());
        // We expect to reach this point for modern Docker versions.
    }


    @Test
    public void extraHostTest() throws IOException {
        BufferedReader br = getReaderForContainerPort80(alpineExtrahost);

        // read hosts file from container
        StringBuffer hosts = new StringBuffer();
        String line = br.readLine();
        while (line != null) {
            hosts.append(line);
            hosts.append("\n");
            line = br.readLine();
        }

        Matcher matcher = Pattern.compile("^192.168.1.10\\s.*somehost", Pattern.MULTILINE).matcher(hosts.toString());
        assertTrue("The hosts file of container contains extra host", matcher.find());
    }

    @Test
    public void createContainerCmdHookTest() {
        // Use random name to avoid the conflicts between the tests
        String randomName = Base58.randomString(5);
        try(
                GenericContainer<?> container = new GenericContainer<>(REDIS_IMAGE)
                        .withCommand("redis-server", "--help")
                        .withCreateContainerCmdModifier(cmd -> cmd.withName("overrideMe"))
                        // Preserves the order
                        .withCreateContainerCmdModifier(cmd -> cmd.withName(randomName))
                        // Allows to override pre-configured values by GenericContainer
                        .withCreateContainerCmdModifier(cmd -> cmd.withCmd("redis-server", "--port", "6379"))
        ) {
            container.start();

            assertEquals("Name is configured", "/" + randomName, container.getContainerInfo().getName());
            assertEquals("Command is configured", "[redis-server, --port, 6379]", Arrays.toString(container.getContainerInfo().getConfig().getCmd()));
        }
    }

    private BufferedReader getReaderForContainerPort80(GenericContainer container) {

        return Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            Socket socket = new Socket(container.getHost(), container.getFirstMappedPort());
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        });
    }

    @Test
    public void addExposedPortAfterWithExposedPortsTest() {
        redis.addExposedPort(8987);
        assertThat("Both ports should be exposed", redis.getExposedPorts().size(), equalTo(2));
        assertTrue("withExposedPort should be exposed", redis.getExposedPorts().contains(REDIS_PORT));
        assertTrue("addExposedPort should be exposed", redis.getExposedPorts().contains(8987));
    }

    @Test
    public void addingExposedPortTwiceShouldNotFail() {
        redis.addExposedPort(8987);
        redis.addExposedPort(8987);
        assertThat("Both ports should be exposed", redis.getExposedPorts().size(), equalTo(2)); // 2 ports = de-duplicated port 8897 and original port 6379
        assertTrue("withExposedPort should be exposed", redis.getExposedPorts().contains(REDIS_PORT));
        assertTrue("addExposedPort should be exposed", redis.getExposedPorts().contains(8987));
    }

    @Test
    public void sharedMemorySetTest() {
        try (GenericContainer containerWithSharedMemory = new GenericContainer<>(TINY_IMAGE)
            .withSharedMemorySize(42L * FileUtils.ONE_MB)
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {

            containerWithSharedMemory.start();

            HostConfig hostConfig = containerWithSharedMemory.getContainerInfo().getHostConfig();
            assertEquals("Shared memory not set on container", hostConfig.getShmSize(), 42L * FileUtils.ONE_MB);
        }
    }
}
