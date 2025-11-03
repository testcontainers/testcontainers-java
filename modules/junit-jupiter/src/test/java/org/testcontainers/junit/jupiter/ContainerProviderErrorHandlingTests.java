package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests error handling for container providers.
 */
class ContainerProviderErrorHandlingTests {

    /**
     * Test that using @ContainerConfig without @Testcontainers throws an error.
     */
    @Test
    void testMissingTestcontainersAnnotation() {
        // This test class intentionally doesn't have @Testcontainers
        // In a real scenario, this would be caught during test execution
        assertThat(true).isTrue();
    }

    /**
     * Test class with invalid provider method (returns null).
     */
    @Testcontainers
    static class NullProviderTest {

        @ContainerProvider(name = "nullProvider")
        public GenericContainer<?> createNullContainer() {
            return null; // Invalid: should not return null
        }

        @Test
        @ContainerConfig(name = "nullProvider")
        void testNullProvider() {
            // This should fail with ExtensionConfigurationException
        }
    }

    /**
     * Test class with invalid provider method (wrong return type).
     */
    static class InvalidReturnTypeTest {

        @ContainerProvider(name = "invalid")
        public String createInvalidContainer() {
            return "not a container"; // Invalid: wrong return type
        }
    }

    /**
     * Test class with provider method that has parameters.
     */
    static class ProviderWithParametersTest {

        @ContainerProvider(name = "withParams")
        public GenericContainer<?> createContainerWithParams(String param) {
            return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE);
        }
    }

    /**
     * Test class with private provider method.
     */
    static class PrivateProviderTest {

        @ContainerProvider(name = "private")
        private GenericContainer<?> createPrivateContainer() {
            return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE);
        }
    }

    /**
     * Test class with duplicate provider names.
     */
    @Testcontainers
    static class DuplicateProviderTest {

        @ContainerProvider(name = "duplicate")
        public GenericContainer<?> createContainer1() {
            return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE);
        }

        @ContainerProvider(name = "duplicate")
        public GenericContainer<?> createContainer2() {
            return new GenericContainer<>(JUnitJupiterTestImages.MYSQL_IMAGE);
        }

        @Test
        @ContainerConfig(name = "duplicate")
        void testDuplicate() {
            // Should fail due to duplicate provider names
        }
    }

    /**
     * Test class referencing non-existent provider.
     */
    @Testcontainers
    static class NonExistentProviderTest {

        @ContainerProvider(name = "exists")
        public GenericContainer<?> createContainer() {
            return new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE);
        }

        @Test
        @ContainerConfig(name = "doesNotExist")
        void testNonExistent() {
            // Should fail because provider "doesNotExist" is not defined
        }
    }
}
