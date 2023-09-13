package org.testcontainers.images;

import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.FakeImagePullPolicy;
import org.testcontainers.utility.MockTestcontainersConfigurationRule;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

public class OverrideImagePullPolicyTest {

    @Rule
    public MockTestcontainersConfigurationRule config = new MockTestcontainersConfigurationRule();

    @Test
    public void simpleConfigurationTest() {
        Mockito
            .doReturn(FakeImagePullPolicy.class.getCanonicalName())
            .when(TestcontainersConfiguration.getInstance())
            .getImagePullPolicy();

        try (DockerRegistryContainer registry = new DockerRegistryContainer()) {
            registry.start();
            GenericContainer<?> container = new GenericContainer<>(registry.createImage()).withExposedPorts(8080);
            container.start();
            assertThat(container.getImage())
                .asString()
                .contains("imagePullPolicy=org.testcontainers.utility.FakeImagePullPolicy");
            container.stop();
        }
    }
}
