package org.testcontainers.junit.jqwik;

import net.jqwik.api.Assume;
import net.jqwik.api.Disabled;
import net.jqwik.api.Example;
import net.jqwik.api.Property;
import net.jqwik.api.lifecycle.AfterProperty;
import org.junit.AssumptionViolatedException;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@Disabled("Assuming within an example based test, causes Jqwik to throw an assertion error, because the example was not" +
    "tested exhaustively. However, the exception is captured and passed to the TestLifeCycleAwareContainerMock.")
@Testcontainers
class TestLifecycleAwareExceptionCapturingTest {
    @TestContainer
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @Example
    void one_example_failing_with_an_exception_or_checking_the_captured_exception() {
        if(startedTestContainer == null) {
            startedTestContainer = testContainer;
            Assume.that(false);
        }

        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertTrue(capturedThrowable instanceof AssertionFailedError);
    }

    @Example
    void another_example_failing_with_an_exception_or_checking_the_captured_exception() {
        if(startedTestContainer == null){
            startedTestContainer = testContainer;
            Assume.that(false);
        }

        Throwable capturedThrowable = startedTestContainer.getCapturedThrowable();
        assertTrue(capturedThrowable instanceof AssertionFailedError);
    }
}
