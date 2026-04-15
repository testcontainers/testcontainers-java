package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link SharedContainersExtension} correctly signals
 * {@link org.testcontainers.lifecycle.TestLifecycleAware#beforeTest} and
 * {@link org.testcontainers.lifecycle.TestLifecycleAware#afterTest} to shared containers.
 */
// The order of @ExtendWith and @SharedContainers is crucial:
// AfterAllVerifier.afterAll() must run after SharedContainersExtension.afterAll()
@ExtendWith(SharedContainersLifecycleAwareTest.AfterAllVerifier.class)
@SharedContainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SharedContainersLifecycleAwareTest {

    @Container
    static final TestLifecycleAwareContainerMock SHARED_CONTAINER = new TestLifecycleAwareContainerMock();

    @BeforeAll
    static void beforeAll() {
        assertThat(SHARED_CONTAINER.getLifecycleMethodCalls())
            .containsExactly(TestLifecycleAwareContainerMock.BEFORE_TEST);
    }

    @Test
    @Order(1)
    void beforeTest_should_be_called_before_tests() {
        assertThat(SHARED_CONTAINER.getLifecycleMethodCalls())
            .containsExactly(TestLifecycleAwareContainerMock.BEFORE_TEST);
    }

    @Test
    @Order(2)
    void afterTest_should_be_called_after_all_tests() {
        // Verified by AfterAllVerifier below after all tests complete
    }

    static class AfterAllVerifier implements AfterAllCallback {

        @Override
        public void afterAll(ExtensionContext context) {
            assertThat(SHARED_CONTAINER.getLifecycleMethodCalls())
                .containsExactly(
                    TestLifecycleAwareContainerMock.BEFORE_TEST,
                    TestLifecycleAwareContainerMock.AFTER_TEST
                );
        }
    }
}
