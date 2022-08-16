import biz.paluch.spinach.DisqueClient;
import biz.paluch.spinach.DisqueURI;
import biz.paluch.spinach.api.AddJobArgs;
import biz.paluch.spinach.api.Job;
import biz.paluch.spinach.api.sync.DisqueCommands;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rnorth on 03/01/2016.
 */
public class SingleDisqueInstanceTest {

    @Rule
    public GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("richnorth/disque:1.0-rc1"))
                                                .withExposedPorts(7711);
    private DisqueCommands<String, String> connection;
    private AddJobArgs retryAfter1Second;

    @Before
    public void setup() {
        DisqueClient client = new DisqueClient(DisqueURI.create(container.getHost(), container.getMappedPort(7711)));
        connection = client.connect().sync();
        retryAfter1Second = AddJobArgs.builder().retry(1, TimeUnit.SECONDS).build();
    }

    @Test
    public void testJobStoreAndRetrieve() {
        connection.addjob("main_queue", "body", 1, TimeUnit.MINUTES);

        Job<String, String> job = connection.getjob("main_queue");

        assertThat(job.getBody()).as("The retrieved job is the same as the one that was added").isEqualTo("body");

        connection.ackjob(job.getId());
    }

    @Test
    public void testFailureToAckJobBeforeTimeout() throws InterruptedException {
        connection.addjob("main_queue", "body", 1, TimeUnit.MINUTES, retryAfter1Second);

        Job<String, String> job = connection.getjob("main_queue");

        assertThat(job.getBody()).as("The retrieved job is the same as the one that was added").isEqualTo("body");

        TimeUnit.SECONDS.sleep(2);

        // The timeout specified here is how long the command will wait for a job to appear
        Job<String, String> job2 = connection.getjob(5, TimeUnit.SECONDS, "main_queue");

        assertThat(job2).as("After re-getting the original job is back on the queue").isNotNull();
        assertThat(job2.getBody()).as("The retrieved job is the same as the one that was added").isEqualTo("body");
    }
}
