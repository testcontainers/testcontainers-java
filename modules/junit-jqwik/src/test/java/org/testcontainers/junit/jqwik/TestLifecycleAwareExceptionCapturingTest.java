package org.testcontainers.junit.jqwik;

import net.jqwik.api.Assume;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import org.opentest4j.AssertionFailedError;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Assuming within an example based test, causes Jqwik to throw an assertion error, because the example was not" +
    "tested exhaustively. However, the exception is captured and passed to the TestLifeCycleAwareContainerMock.")
@Testcontainers
class TestLifecycleAwareExceptionCapturingTest {
    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @Example
    void one_example_failing_with_an_exception_or_checking_the_captured_exception() {
        if(startedTestContainer == null) {
            startedTestContainer = testContainer;
            Assume.that(false);
        }

        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertThat(capturedThrowable).isInstanceOf(AssertionFailedError.class);
    }

    @Example
    void another_example_failing_with_an_exception_or_checking_the_captured_exception() {
        if(startedTestContainer == null){
            startedTestContainer = testContainer;
            Assume.that(false);
        }

        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertThat(capturedThrowable).isInstanceOf(AssertionFailedError.class);
    }
}
