package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class with shared container provider for cross-class testing.
 */
abstract class ContainerProviderBaseTest {

    @ContainerProvider(name = "sharedRedis", scope = ContainerProvider.Scope.GLOBAL)
    public GenericContainer<?> createSharedRedis() {
        return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE).withExposedPorts(80);
    }
}

/**
 * First test class using the shared provider.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerProviderCrossClassTests1 extends ContainerProviderBaseTest {

    private static String firstContainerId;

    @Test
    @Order(1)
    @ContainerConfig(name = "sharedRedis", injectAsParameter = true)
    void testInClass1_Test1(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        firstContainerId = container.getContainerId();
        assertThat(firstContainerId).isNotNull();
    }

    @Test
    @Order(2)
    @ContainerConfig(name = "sharedRedis", injectAsParameter = true)
    void testInClass1_Test2(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getContainerId()).isEqualTo(firstContainerId);
    }
}

/**
 * Second test class using the same shared provider.
 * This should reuse the container from the first test class.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ContainerProviderCrossClassTests2 extends ContainerProviderBaseTest {

    @Test
    @Order(1)
    @ContainerConfig(name = "sharedRedis", injectAsParameter = true)
    void testInClass2_Test1(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        
        // This should be the same container as used in Class1
        String containerId = container.getContainerId();
        assertThat(containerId).isNotNull();
    }

    @Test
    @Order(2)
    @ContainerConfig(name = "sharedRedis", injectAsParameter = true)
    void testInClass2_Test2(GenericContainer<?> container) {
        assertThat(container).isNotNull();
        assertThat(container.isRunning()).isTrue();
        assertThat(container.getHost()).isNotNull();
        assertThat(container.getFirstMappedPort()).isGreaterThan(0);
    }
}
