package org.testcontainers.junit.jupiter;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opentest4j.TestAbortedException;

import static org.assertj.core.api.Assertions.assertThat;

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
        Assumptions.assumeThat(true).isFalse();
    }

    @Test
    @Order(2)
    void should_have_captured_thrownException() {
        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertThat(capturedThrowable).isInstanceOf(TestAbortedException.class);
        assertThat(capturedThrowable.getMessage()).isEqualTo("got: <false>, expected: is <true>");
    }
}
