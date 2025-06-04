package org.testcontainers.junit.vintage;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class TestcontainersTest {

    private static final Statement PASSING_STATEMENT = new Statement() {
        @Override
        public void evaluate() throws Throwable {}
    };

    private static final Statement FAILING_STATEMENT = new Statement() {
        @Override
        public void evaluate() throws Throwable {
            throw new TestException();
        }
    };

    @After
    public void resetIntegrationTest() {
        IntegrationTest.reset();
    }

    @Test
    public void statementDelegateDoesNotThrow() throws Throwable {
        // Arrange
        FakeTest fakeTest = new FakeTest();
        Testcontainers containers = new Testcontainers(fakeTest);
        Statement statement = containers.apply(PASSING_STATEMENT, FakeTest.TEST_DESCRIPTION);
        assertThat(fakeTest.testContainer.getLifecycleMethodCalls()).isEmpty();

        // Act - evaluate statement
        statement.evaluate();

        // Assert
        assertThat(fakeTest.testContainer.getLifecycleMethodCalls())
            .containsExactly(
                TestLifecycleAwareContainerMock.START,
                TestLifecycleAwareContainerMock.BEFORE_TEST,
                TestLifecycleAwareContainerMock.AFTER_TEST,
                TestLifecycleAwareContainerMock.STOP
            );
        assertThat(fakeTest.testContainer.getCapturedThrowable()).isNull();
        assertThat(fakeTest.testContainer.getLifecycleFilesystemFriendlyNames()).isEqualTo(FakeTest.FRIENDLY_NAMES);
    }

    @Test
    public void statementDelegateThrows() {
        // Arrange
        FakeTest fakeTest = new FakeTest();
        Testcontainers containers = new Testcontainers(fakeTest);
        Statement statement = containers.apply(FAILING_STATEMENT, FakeTest.TEST_DESCRIPTION);
        assertThat(fakeTest.testContainer.getLifecycleMethodCalls()).isEmpty();

        // Act - evaluate statement
        assertThatExceptionOfType(TestException.class).isThrownBy(statement::evaluate);

        // Assert
        assertThat(fakeTest.testContainer.getLifecycleMethodCalls())
            .containsExactly(
                TestLifecycleAwareContainerMock.START,
                TestLifecycleAwareContainerMock.BEFORE_TEST,
                TestLifecycleAwareContainerMock.AFTER_TEST,
                TestLifecycleAwareContainerMock.STOP
            );
        assertThat(fakeTest.testContainer.getCapturedThrowable()).isNotNull();
        assertThat(fakeTest.testContainer.getLifecycleFilesystemFriendlyNames()).isEqualTo(FakeTest.FRIENDLY_NAMES);
    }

    @Test
    public void integrationTestsPass() throws Exception {
        IntegrationTest.enabled = true;

        Result result = JUnitCore.runClasses(IntegrationTest.class);

        verifyNoFailures(result);
        assertThat(IntegrationTest.testsStarted)
            .withFailMessage("No tests in IntegrationTests were run")
            .isGreaterThan(0);
    }

    /** Test class used for tests that directly call the Testcontainers rule. */
    private static class FakeTest {

        static final Description TEST_DESCRIPTION = Description.createTestDescription(FakeTest.class, "boom");

        static final List<String> FRIENDLY_NAMES = Collections.singletonList(
            FailureDetectingExternalResource.toTestDescription(TEST_DESCRIPTION).getFilesystemFriendlyName()
        );

        @Container
        private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();
    }

    /** Integration tests for verifying behavior around container discovery. */
    public static class IntegrationTest {

        static final List<String> FRIENDLY_NAMES = Collections.singletonList(
            FailureDetectingExternalResource
                .toTestDescription(Description.createTestDescription(IntegrationTest.class, "containerStarted"))
                .getFilesystemFriendlyName()
        );

        static int testsStarted = 0;

        static boolean enabled = false;

        /** Ensures that the tests in this class are not run directly by gradle. */
        final TestRule skipWhenDisabled = new TestRule() {
            @Override
            public final Statement apply(Statement base, Description description) {
                return enabled ? base : PASSING_STATEMENT;
            }
        };

        /** The class under test; this isn't annotated by @Rule because it is run by "testRuleChain". */
        final Testcontainers containers = new Testcontainers(this);

        final TestRule verifyPreAndPostConditions = new TestWatcher() {
            @Override
            protected void starting(Description description) {
                forEachTestContainer(container -> {
                    assertThat(container.getLifecycleMethodCalls()).isEmpty();
                });
            }

            @Override
            protected void finished(Description description) {
                forEachTestContainer(container -> {
                    assertThat(container.getLifecycleMethodCalls())
                        .containsExactly(
                            TestLifecycleAwareContainerMock.START,
                            TestLifecycleAwareContainerMock.BEFORE_TEST,
                            TestLifecycleAwareContainerMock.AFTER_TEST,
                            TestLifecycleAwareContainerMock.STOP
                        );
                });
            }
        };

        @Rule
        public final RuleChain testRuleChain = RuleChain
            .outerRule(skipWhenDisabled)
            .around(verifyPreAndPostConditions)
            .around(containers);

        static void reset() {
            enabled = false;
            testsStarted = 0;
        }

        @Container
        private final TestLifecycleAwareContainerMock testContainer1 = new TestLifecycleAwareContainerMock();

        @Container
        private final TestLifecycleAwareContainerMock testContainer2 = new TestLifecycleAwareContainerMock();

        @Test
        public void containerStarted() {
            testsStarted++;

            forEachTestContainer(container -> {
                assertThat(container.getLifecycleMethodCalls())
                    .containsExactly(
                        TestLifecycleAwareContainerMock.START,
                        TestLifecycleAwareContainerMock.BEFORE_TEST
                    );
            });
        }

        private void forEachTestContainer(Consumer<TestLifecycleAwareContainerMock> callback) {
            callback.accept(testContainer1);
            callback.accept(testContainer2);
        }
    }

    private static void verifyNoFailures(Result result) throws Exception {
        List<Throwable> exceptions = result
            .getFailures()
            .stream()
            .map(Failure::getException)
            .collect(Collectors.toList());
        MultipleFailureException.assertEmpty(exceptions);
    }

    static class TestException extends RuntimeException {}
}
