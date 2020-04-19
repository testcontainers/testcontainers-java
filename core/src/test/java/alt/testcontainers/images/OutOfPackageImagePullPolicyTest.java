package alt.testcontainers.images;

import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.AbstractImagePullPolicy;
import org.testcontainers.images.ImageData;
import org.testcontainers.utility.DockerImageName;

public class OutOfPackageImagePullPolicyTest {
    @Test
    public void shouldSupportCustomPoliciesOutOfTestContainersPackage() {
        try (
            GenericContainer<?> container = new GenericContainer<>()
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
