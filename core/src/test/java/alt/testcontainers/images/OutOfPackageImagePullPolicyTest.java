package alt.testcontainers.images;

import org.junit.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.AbstractImagePullPolicy;
import org.testcontainers.images.ImageData;
import org.testcontainers.utility.DockerImageName;

public class OutOfPackageImagePullPolicyTest {

    @Test
    public void shouldSupportCustomPoliciesOutOfTestcontainersPackage() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withImagePullPolicy(
                    new AbstractImagePullPolicy() {
                        @Override
                        protected boolean shouldPullCached(DockerImageName imageName, ImageData localImageData) {
                            return false;
                        }
                    }
                )
        ) {
            container.withStartupCheckStrategy(new OneShotStartupCheckStrategy());
            container.start();
        }
    }
}
