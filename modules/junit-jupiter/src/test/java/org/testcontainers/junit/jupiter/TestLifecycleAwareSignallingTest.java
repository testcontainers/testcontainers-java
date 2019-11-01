package org.testcontainers.junit.jupiter;

import org.junit.Assume;
import org.junit.AssumptionViolatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

// The order of @ExtendsWith and @Testcontainers is crucial in order for the static case to be tested
@ExtendWith({TestLifecycleAwareSignallingTest.SharedContainerAfterAllTestExtension.class})
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class TestLifecycleAwareSignallingTest {

    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    @Container
    private static final TestLifecycleAwareContainerMock SHARED_CONTAINER = new TestLifecycleAwareContainerMock();

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @BeforeAll
    static void beforeAll() {
        assertEquals(1, SHARED_CONTAINER.getNumBeforeTestsCalls());
    }

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

    @Test
    @Order(3)
    void should_call_beforeTest_before_afterTest() {
        long beforeTestCalledAtMillis = startedTestContainer.getBeforeTestCalledAtMillis();
        long afterTestCalledAtMillis = startedTestContainer.getAfterTestCalledAtMillis();
        assertTrue(beforeTestCalledAtMillis < afterTestCalledAtMillis);
    }

    @Test
    @Order(4)
    void failing_test_should_pass_throwable_to_testContainer() {
        startedTestContainer = testContainer;
        assumeTrue(false);
    }

    @Test
    @Order(5)
    void should_have_captured_thrownException() {
        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertNotNull(capturedThrowable);
        assertTrue(capturedThrowable instanceof AssumptionViolatedException);
        assertEquals("got: <false>, expected: is <true>", capturedThrowable.getMessage());
    }

    static class SharedContainerAfterAllTestExtension implements AfterAllCallback {

        @Override
        public void afterAll(ExtensionContext context) {
            assertEquals(1, SHARED_CONTAINER.getNumBeforeTestsCalls());
            assertEquals(1, SHARED_CONTAINER.getNumAfterTestsCalls());
            long beforeTestCalledAtMillis = SHARED_CONTAINER.getBeforeTestCalledAtMillis();
            long afterTestCalledAtMillis = SHARED_CONTAINER.getAfterTestCalledAtMillis();
            assertTrue(beforeTestCalledAtMillis < afterTestCalledAtMillis);
        }
    }
}
