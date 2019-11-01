package generic;

import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.image.pull.policy.PullPolicy;

public class ImagePullPolicyTest {

    // image_pull_policy {
    @ClassRule
    public static GenericContainer containerAlwaysPull = new GenericContainer("redis:3.0.2")
        .withImagePullPolicy(PullPolicy.alwaysPull());
    // }

    // custom_image_pull_policy {
    @ClassRule
    public static GenericContainer containerCustomPullPolicy = new GenericContainer("redis:3.0.2")
        .withImagePullPolicy(image -> System.getenv("ALWAYS_PULL_IMAGE") != null);
    // }

    @Test
    public void testStartup() {
        assertTrue(containerAlwaysPull.isRunning()); // good enough to check that the container started listening
        assertTrue(containerCustomPullPolicy.isRunning()); // good enough to check that the container started listening
    }

}
