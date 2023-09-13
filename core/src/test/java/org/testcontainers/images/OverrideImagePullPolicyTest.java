package org.testcontainers.images;

import org.junit.After;
import org.junit.Before;
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

    private ImagePullPolicy originalInstance;

    private ImagePullPolicy originalDefaultImplementation;

    @Before
    public void setUp() {
        this.originalInstance = PullPolicy.instance;
        this.originalDefaultImplementation = PullPolicy.defaultImplementation;
        PullPolicy.instance = null;
        PullPolicy.defaultImplementation = Mockito.mock(ImagePullPolicy.class);
    }

    @After
    public void tearDown() {
        PullPolicy.instance = originalInstance;
        PullPolicy.defaultImplementation = originalDefaultImplementation;
    }

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
