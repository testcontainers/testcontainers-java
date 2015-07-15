package org.rnorth.testcontainers.junit;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.rabbitmq.client.*;
import org.bson.Document;
import org.junit.ClassRule;
import org.junit.Test;
import org.redisson.Config;
import org.redisson.Redisson;
import org.rnorth.testcontainers.utility.Retryables;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testpackage.VisibleAssertions.assertEquals;
import static org.testpackage.VisibleAssertions.assertTrue;

/**
 * Tests for GenericContainerRules
 */
public class GenericContainerRuleTest {

    public static final String REDIS_PORT = "6379";

    private static final String RABBIQMQ_TEST_EXCHANGE = "TestExchange";
    private static final String RABBITMQ_TEST_ROUTING_KEY = "TestRoutingKey";
    private static final String RABBITMQ_TEST_MESSAGE = "Hello world";
    private static final String RABBITMQ_PORT = "5672";
    private static final String MONGO_PORT = "27017";

    @ClassRule
    public static GenericContainerRule redis = new GenericContainerRule("redis:3.0.2")
                                            .withExposedPorts(REDIS_PORT);

    @ClassRule
    public static GenericContainerRule rabbitMq = new GenericContainerRule("rabbitmq:3.5.3")
                                            .withExposedPorts(RABBITMQ_PORT);

    @ClassRule
    public static GenericContainerRule mongo = new GenericContainerRule("mongo:3.1.5")
                                            .withExposedPorts(MONGO_PORT);

    @ClassRule
    public static GenericContainerRule alpine = new GenericContainerRule("alpine:3.2")
                                                    .withExposedPorts(80)
                                                    .withEnv("MAGIC_NUMBER", "42")
                                                    .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");

    @Test
    public void simpleRedisTest() {
        String ipAddress = redis.getIpAddress();
        String port = redis.getPort(REDIS_PORT);

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
        factory.setHost(rabbitMq.getIpAddress());
        factory.setPort(Integer.valueOf(rabbitMq.getPort(RABBITMQ_PORT)));
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
        assertTrue("The message was received", Retryables.retryUntilSuccess(5, TimeUnit.SECONDS, new Retryables.UnreliableSupplier<Boolean>() {
            @Override
            public Boolean get() throws Exception {
                if (!messageWasReceived[0]) {
                    throw new IllegalStateException("Message not received yet");
                }
                return true;
            }
        }));
    }

    @Test
    public void simpleMongoDbTest() {
        MongoClient mongoClient = new MongoClient(mongo.getIpAddress(), Integer.valueOf(mongo.getPort(MONGO_PORT)));
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
        Socket socket = new Socket(alpine.getIpAddress(), Integer.valueOf(alpine.getPort("80")));
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        String line = br.readLine();

        assertEquals("An environment variable can be passed into a command", "42", line);
    }
}
