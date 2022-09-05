package org.testcontainers.junit4;

import org.junit.AssumptionViolatedException;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

// The order is crucial in order for the tests
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestLifecycleAwareExceptionCapturingTest {

    @Rule
    public final ContainerRule<TestLifecycleAwareContainerMock> testContainer = new ContainerRule<>(
        new TestLifecycleAwareContainerMock()
    );

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @Test
    public void failing_test_should_pass_throwable_to_testContainer() {
        startedTestContainer = testContainer.get();
        // Force an exception that is captured by the test container without failing the test itself
        assumeTrue(false);
    }

    @Test
    public void should_have_captured_thrownException() {
        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertThat(capturedThrowable).isInstanceOf(AssumptionViolatedException.class);
        assertThat(capturedThrowable.getMessage()).isEqualTo("got: <false>, expected: is <true>");
    }
}
