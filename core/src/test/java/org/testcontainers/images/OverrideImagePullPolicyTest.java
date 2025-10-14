package org.testcontainers.images;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.DockerRegistryContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.FakeImagePullPolicy;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class OverrideImagePullPolicyTest {

    private ImagePullPolicy originalInstance;

    private ImagePullPolicy originalDefaultImplementation;

    @BeforeEach
    public void setUp() {
        this.originalInstance = PullPolicy.instance;
        this.originalDefaultImplementation = PullPolicy.defaultImplementation;
        PullPolicy.instance = null;
        PullPolicy.defaultImplementation = Mockito.mock(ImagePullPolicy.class);
    }

    @AfterEach
    public void tearDown() {
        PullPolicy.instance = originalInstance;
        PullPolicy.defaultImplementation = originalDefaultImplementation;
    }

    @Test
    void simpleConfigurationTest() {
        Mockito
            .doReturn(FakeImagePullPolicy.class.getCanonicalName())
            .when(TestcontainersConfiguration.getInstance())
            .getImagePullPolicy();

        try (DockerRegistryContainer registry = new DockerRegistryContainer()) {
            registry.start();
            GenericContainer<?> container = new GenericContainer<>(registry.createImage()).withExposedPorts(8080);
            container.start();
            assertThat(container.getImage().imagePullPolicy).isInstanceOf(FakeImagePullPolicy.class);
            container.stop();
        }
    }

    @Test
    void alwaysPullConfigurationTest() {
        Mockito
            .doReturn(AlwaysPullPolicy.class.getCanonicalName())
            .when(TestcontainersConfiguration.getInstance())
            .getImagePullPolicy();

        try (DockerRegistryContainer registry = new DockerRegistryContainer()) {
            registry.start();
            GenericContainer<?> container = new GenericContainer<>(registry.createImage()).withExposedPorts(8080);
            container.start();
            assertThat(container.getImage().imagePullPolicy).isInstanceOf(AlwaysPullPolicy.class);
            container.stop();
        }
    }
}
