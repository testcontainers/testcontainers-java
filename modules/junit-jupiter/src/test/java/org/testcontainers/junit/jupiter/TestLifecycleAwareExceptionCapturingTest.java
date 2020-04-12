package org.testcontainers.junit.jupiter;

import org.junit.AssumptionViolatedException;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

// The order of @ExtendsWith and @Testcontainers is crucial in order for the tests
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class TestLifecycleAwareExceptionCapturingTest {
    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @Test
    @Order(1)
    void failing_test_should_pass_throwable_to_testContainer() {
        startedTestContainer = testContainer;
        // Force an exception that is captured by the test container without failing the test itself
        assumeTrue(false);
    }

    @Test
    @Order(2)
    void should_have_captured_thrownException() {
        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertTrue(capturedThrowable instanceof AssumptionViolatedException);
        assertEquals("got: <false>, expected: is <true>", capturedThrowable.getMessage());
    }
}
