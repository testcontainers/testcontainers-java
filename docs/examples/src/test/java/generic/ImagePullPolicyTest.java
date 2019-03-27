package generic;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.image.pull.policy.PullPolicy;

public class ImagePullPolicyTest {

    // image_pull_policy {
    public GenericContainer containerAlwaysPull = new GenericContainer("alpine:3.6")
        .withImagePullPolicy(PullPolicy.Always());
    // }

    // custom_image_pull_policy {
    public GenericContainer containerCustomPullPolicy = new GenericContainer("alpine:3.6")
        .withImagePullPolicy(image -> System.getenv("ALWAYS_PULL_IMAGE") != null);
    // }

}
