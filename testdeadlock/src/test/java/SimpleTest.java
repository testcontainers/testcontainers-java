import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * Created by rnorth on 18/02/2017.
 */
public class SimpleTest {

    static {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
            System.err.println("Thread dump at: " + new Date());
            Thread.getAllStackTraces().forEach((thread, stacktrace) -> {
                System.err.println(" Stack trace for thread: " + thread.getName() + "\n" + Arrays.toString(stacktrace) + "\n\n");
            });
        }, 30, 10, TimeUnit.SECONDS);
    }

    @Rule
    public GenericContainer mongo = new GenericContainer("mongo:3.2")
            .withExposedPorts(27017);

    @Test
    public void testA() {
        final MongoClient client = new MongoClient(mongo.getContainerIpAddress(), mongo.getMappedPort(27017));
        final MongoDatabase db = client.getDatabase("db");
        db.createCollection("books");
        final MongoCollection<Document> booksCollection = db.getCollection("books");

        final Document document = new Document("key1", "value1");
        booksCollection.insertOne(document);

        booksCollection.find().forEach((Block<? super Document>) System.out::println);

        assertEquals("one record is found", 1L, booksCollection.count());
    }
}
