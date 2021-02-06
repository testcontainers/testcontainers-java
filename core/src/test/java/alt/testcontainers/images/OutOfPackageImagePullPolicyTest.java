package alt.testcontainers.images;

import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.AbstractImagePullPolicy;
import org.testcontainers.images.ImageData;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.TestImages.TINY_IMAGE;

public class OutOfPackageImagePullPolicyTest {
    @Test
    public void shouldSupportCustomPoliciesOutOfTestContainersPackage() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withImagePullPolicy(new AbstractImagePullPolicy() {
                    @Override
                    protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                        return false;
                    }
                })
        ) {
            container.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            container.start();
        }
    }

}
