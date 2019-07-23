package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.Assert.assertEquals;

// The order of @ExtendsWith and @Testcontainers is crucial in order for the static case to be tested
@ExtendWith({TestLifecycleAwareSignallingTest.ExtensionSharedContainerTestExtension.class})
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class TestLifecycleAwareSignallingTest {

    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    @Container
    private static final TestLifecycleAwareContainerMock SHARED_CONTAINER = new TestLifecycleAwareContainerMock();

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @Test
    @Order(1)
    void should_call_beforeTest_on_TestLifecycleAware_container() {
        assertEquals(1, testContainer.getNumBeforeTestsCalls());
        startedTestContainer = testContainer;
    }

    @Test
    @Order(2)
    void should_call_afterTest_on_TestLifecycleAware_container() {
        assertEquals(1, startedTestContainer.getNumAfterTestsCalls());
    }

    static class ExtensionSharedContainerTestExtension implements AfterAllCallback {

        @Override
        public void afterAll(ExtensionContext context) {
            assertEquals(1, SHARED_CONTAINER.getNumAfterTestsCalls());
        }
    }
}


