package org.testcontainers.junit;

import com.google.common.util.concurrent.Uninterruptibles;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.*;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.redisson.Config;
import org.redisson.Redisson;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.GenericContainer;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.containers.BindMode.READ_ONLY;

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
    public static GenericContainer redis = new GenericContainer("redis:3.0.2")
            .withExposedPorts(REDIS_PORT);

    /**
     * RabbitMQ
     */
    @ClassRule
    public static GenericContainer rabbitMq = new GenericContainer("rabbitmq:3.5.3")
            .withExposedPorts(RABBITMQ_PORT);

    /**
     * MongoDB
     */
    @ClassRule
    public static GenericContainer mongo = new GenericContainer("mongo:3.1.5")
            .withExposedPorts(MONGO_PORT);

    /**
     * Pass an environment variable to the container, then run a shell script that exposes the variable in a quick and
     * dirty way for testing.
     */
    @ClassRule
    public static GenericContainer alpineEnvVar = new GenericContainer("alpine:3.2")
            .withExposedPorts(80)
            .withEnv("MAGIC_NUMBER", "42")
            .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");

    /**
     * Map a file on the classpath to a file in the container, and then expose the content for testing.
     */
    @ClassRule
    public static GenericContainer alpineClasspathResource = new GenericContainer("alpine:3.2")
            .withExposedPorts(80)
            .withClasspathResourceMapping("mappable-resource/test-resource.txt", "/content.txt", READ_ONLY)
            .withCommand("/bin/sh", "-c", "while true; do cat /content.txt | nc -l -p 80; done");


    @Test
    public void simpleRedisTest() {
        String ipAddress = redis.getContainerIpAddress();
        Integer port = redis.getMappedPort(REDIS_PORT);

        // Use Redisson to obtain a List that is backed by Redis
        Config redisConfig = new Config();
        redisConfig.useSingleServer().setAddress(ipAddress + ":" + port);

        Redisson redisson = Redisson.create(redisConfig);

        List<String> testList = redisson.getList("test");
        testList.add("foo");
        testList.add("bar");
        testList.add("baz");

        List<String> testList2 = redisson.getList("test");
        assertEquals("The list contains the expected number of items (redis is working!)", 3, testList2.size());
        assertTrue("The list contains an item that was put in (redis is working!)", testList2.contains("foo"));
        assertTrue("The list contains an item that was put in (redis is working!)", testList2.contains("bar"));
        assertTrue("The list contains an item that was put in (redis is working!)", testList2.contains("baz"));
    }

    @Test
    public void simpleRabbitMqTest() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitMq.getContainerIpAddress());
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
        MongoClient mongoClient = new MongoClient(mongo.getContainerIpAddress(), mongo.getMappedPort(MONGO_PORT));
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
        BufferedReader br = Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            Socket socket = new Socket(alpineEnvVar.getContainerIpAddress(), alpineEnvVar.getMappedPort(80));
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        });

        String line = br.readLine();

        assertEquals("An environment variable can be passed into a command", "42", line);
    }

    @Test
    public void customClasspathResourceMappingTest() throws IOException {
        BufferedReader br = Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            Socket socket = new Socket(alpineClasspathResource.getContainerIpAddress(), alpineClasspathResource.getMappedPort(80));
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        });

        String line = br.readLine();

        assertEquals("Resource on the classpath can be mapped using calls to withClasspathResourceMapping", "FOOBAR", line);
    }

    @Test
    public void exceptionThrownWhenMappedPortNotFound() throws IOException {
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
}
