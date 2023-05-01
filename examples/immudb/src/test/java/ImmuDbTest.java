import io.codenotary.immudb4j.Entry;
import io.codenotary.immudb4j.ImmuClient;
import io.codenotary.immudb4j.exceptions.VerificationException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.assertj.core.api.Assertions.assertThat;

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
    public static final GenericContainer<?> immuDbContainer =  new GenericContainer<>("codenotary/immudb:1.3")
        .withExposedPorts(IMMUDB_PORT)
        .waitingFor(Wait.forLogMessage(".*Web API server enabled.*", 1));

    // ImmuClient used to interact with the DB
    private ImmuClient immuClient;

    @BeforeAll
    public static void beforeAll() {
        immuDbContainer.start();

        }

    @BeforeEach
    public void setUp() {
        this.immuClient =
            ImmuClient
                .newBuilder()
                .withServerUrl(immuDbContainer.getHost())
                .withServerPort(immuDbContainer.getMappedPort(IMMUDB_PORT))
                .build();
        this.immuClient.openSession(IMMUDB_DATABASE, IMMUDB_USER, IMMUDB_PASSWORD);
    }

    @AfterEach
    public void tearDown() {
        this.immuClient.closeSession();
    }

    @AfterAll
    public static void afterAll() {
        immuDbContainer.stop();
    }

    @Test
    public void testGetValue() {
        try {
            immuClient.set("test1", "test2".getBytes());

            Entry entry = immuClient.verifiedGet("test1");

            if (entry != null) {
                byte[] value = entry.getValue();
                assertThat(new String(value)).isEqualTo("test2");
            } else {
                Assertions.fail("Entry Null");
            }
        } catch (VerificationException e) {
            Assertions.fail(e.getMessage());
        }
    }
}
