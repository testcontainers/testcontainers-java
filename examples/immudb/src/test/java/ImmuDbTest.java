import java.util.List;

import com.mycompany.immudb.ImmuDbClient;
import com.mycompany.immudb.ImmuDbClientImpl;
import io.codenotary.immudb4j.Entry;
import io.codenotary.immudb4j.ImmuClient;
import io.codenotary.immudb4j.exceptions.CorruptedDataException;
import io.codenotary.immudb4j.exceptions.VerificationException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Test class for the ImmuDbClient.
 */
public class ImmuDbTest {

    // Default port for the ImmuDb server
    private static final int IMMUDB_PORT = 3322;
    // Default username for the ImmuDb server
    private final String IMMUDB_USER = "immudb";
    // Default password for the ImmuDb server
    private final String IMMUDB_PASSWORD = "immudb";
    // Default database name for the ImmuDb server
    private final String IMMUDB_DATABASE = "defaultdb";

    // Test container for the ImmuDb database, with the latest version of the image and exposed port
    @ClassRule
    public static final GenericContainer<?> immuDbContainer = new GenericContainer<>(DockerImageName.parse("codenotary/immudb:latest"))
        .withExposedPorts(IMMUDB_PORT).withReuse(true);
    // Interface for the ImmuDbClient
    private ImmuDbClient immuDbClient;
    // ImmuClient used to interact with the DB
    private ImmuClient immuClient;

    /**
     * Set up the test, by creating the ImmuDbClient and connecting to the DB.
     */
    @Before
    public void setUp() {
        this.immuClient = ImmuClient.newBuilder()
            .withServerUrl(immuDbContainer.getHost())
            .withServerPort(immuDbContainer.getMappedPort(IMMUDB_PORT))
            .build();
        this.immuClient.login(IMMUDB_USER, IMMUDB_PASSWORD);
        this.immuClient.useDatabase(IMMUDB_DATABASE);

        // Initialize the ImmuDbClient
        this.immuDbClient = new ImmuDbClientImpl(this.immuClient);
    }

    /**
     * Test method for the putValue.
     */
    @Test
    public void testPutValue() {
        try {
            this.immuDbClient.putValue("key", "value");
        }
        catch (CorruptedDataException e) {
            Assert.fail();
        }
    }

    /**
     * Test method for the getValue.
     */
    @Test
    public void testGetValue() {
        try {
            this.immuDbClient.putValue("test1", "test2");
            Assert.assertEquals("test2", this.immuDbClient.getValue("test1"));
        }
        catch (CorruptedDataException | VerificationException e) {
            Assert.fail();
        }
    }

    /**
     * Test method for the getAllValues.
     */
    @Test
    public void testGetAllValues() {
        try {
            this.immuDbClient.putValue("test1", "value1");
            this.immuDbClient.putValue("test2", "value2");
            this.immuDbClient.putValue("test3", "value3");

            final List<Entry> allValues = this.immuDbClient.getAllValues(List.of("test1", "test2", "test3"));
            for (Entry e : allValues) {
                byte[] k = e.getKey();
                byte[] v = e.getValue();

                String key = new String(k);
                String value = new String(v);
                switch (key) {
                case "test1":
                    Assert.assertEquals("value1", value);
                    break;
                case "test2":
                    Assert.assertEquals("value2", value);
                    break;
                case "test3":
                    Assert.assertEquals("value3", value);
                    break;
                default:
                    Assert.fail();
                }
            }
        }
        catch (CorruptedDataException e) {
            Assert.fail();
        }
    }

    /**
     * Test method for the getHistory.
     */
    @Test
    public void testGetHistoryValues(){
        try {
            String historyKey = "history1";
            this.immuDbClient.putValue(historyKey, "value1");
            this.immuDbClient.putValue(historyKey, "value2");
            this.immuDbClient.putValue(historyKey, "value3");

            final List<Entry> allValues = this.immuDbClient.getHistoryValues(historyKey, 10);

            Assert.assertEquals("history1", new String(allValues.get(0).getKey()));
            Assert.assertEquals("value1", new String(allValues.get(0).getValue()));

            Assert.assertEquals("history1", new String(allValues.get(1).getKey()));
            Assert.assertEquals("value2", new String(allValues.get(1).getValue()));

            Assert.assertEquals("history1", new String(allValues.get(2).getKey()));
            Assert.assertEquals("value3", new String(allValues.get(2).getValue()));
        }
        catch (CorruptedDataException e) {
            Assert.fail();
        }
    }

    /**
     * Test method for the scan.
     */
    @Test
    public void testScan(){
        try {
            String prefix = "scan";
            String key1 = prefix + "1";
            String key2 = prefix + "2";

            this.immuDbClient.putValue(key1, "value1");
            this.immuDbClient.putValue(key2, "value2");

            final List<Entry> allValues = this.immuDbClient.scanKeys(prefix, 2);

            Assert.assertEquals("scan1", new String(allValues.get(0).getKey()));
            Assert.assertEquals("value1", new String(allValues.get(0).getValue()));

            Assert.assertEquals("scan2", new String(allValues.get(1).getKey()));
            Assert.assertEquals("value2", new String(allValues.get(1).getValue()));
        }
        catch (CorruptedDataException e) {
            Assert.fail();
        }
    }
}
