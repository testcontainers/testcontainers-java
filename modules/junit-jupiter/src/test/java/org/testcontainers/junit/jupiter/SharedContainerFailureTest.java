package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SharedContainerFailureTest {

    @Test
    void retainsFirstMethodFailureWithoutChangingInstanceCallbacks() {
        execute(MethodFailures.class, 2);

        assertThat(MethodFailures.SHARED.getCapturedThrowable()).isSameAs(MethodFailures.FIRST);
        assertSingleLifecycle(MethodFailures.SHARED);
        assertThat(MethodFailures.LOCAL).hasSize(3);
        assertThat(MethodFailures.LOCAL.get(0).getCapturedThrowable()).isSameAs(MethodFailures.FIRST);
        assertThat(MethodFailures.LOCAL.get(1).getCapturedThrowable()).isNull();
        assertThat(MethodFailures.LOCAL.get(2).getCapturedThrowable()).isSameAs(MethodFailures.SECOND);
        MethodFailures.LOCAL.forEach(this::assertSingleLifecycle);
    }

    @Test
    void nestedFailureReachesEnclosingSharedContainerButNotSibling() {
        execute(NestedFailures.class, 1);

        assertThat(NestedFailures.SHARED.getCapturedThrowable()).isSameAs(NestedFailures.FAILURE);
        assertThat(FailingNestedContainers.SHARED.getCapturedThrowable()).isSameAs(NestedFailures.FAILURE);
        assertThat(PassingNestedContainers.SHARED.getCapturedThrowable()).isNull();
        assertSingleLifecycle(NestedFailures.SHARED);
        assertSingleLifecycle(FailingNestedContainers.SHARED);
        assertSingleLifecycle(PassingNestedContainers.SHARED);
    }

    @Test
    void classFailureTakesPrecedenceOverMethodFailure() {
        execute(ClassFailure.class, 2);

        assertThat(ClassFailure.SHARED.getCapturedThrowable()).isSameAs(ClassFailure.CLASS_FAILURE);
        assertSingleLifecycle(ClassFailure.SHARED);
    }

    @Test
    void nestedClassFailureReachesEnclosingSharedContainer() {
        execute(NestedClassFailure.class, 1);

        assertThat(NestedClassFailure.SHARED.getCapturedThrowable()).isSameAs(NestedClassFailure.FAILURE);
        assertSingleLifecycle(NestedClassFailure.SHARED);
    }

    @Test
    void successfulClassHasNoFailure() {
        execute(Successful.class, 0);

        assertThat(Successful.SHARED.getCapturedThrowable()).isNull();
        assertSingleLifecycle(Successful.SHARED);
    }

    private void execute(Class<?> testClass, long failures) {
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        LauncherFactory
            .create()
            .execute(
                LauncherDiscoveryRequestBuilder
                    .request()
                    .selectors(DiscoverySelectors.selectClass(testClass))
                    .configurationParameter("junit.jupiter.execution.parallel.enabled", "false")
                    .build(),
                listener
            );
        assertThat(listener.getSummary().getTotalFailureCount()).isEqualTo(failures);
        assertThat(listener.getSummary().getTestsStartedCount()).isPositive();
    }

    private void assertSingleLifecycle(TestLifecycleAwareContainerMock container) {
        assertThat(container.getLifecycleMethodCalls()).containsExactly("beforeTest", "afterTest");
    }

    @Testcontainers
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    static class MethodFailures {

        static final RuntimeException FIRST = new RuntimeException("first method failure");

        static final RuntimeException SECOND = new RuntimeException("second method failure");

        static final List<TestLifecycleAwareContainerMock> LOCAL = new ArrayList<>();

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();

        @Container
        final TestLifecycleAwareContainerMock local = new TestLifecycleAwareContainerMock();

        MethodFailures() {
            LOCAL.add(local);
        }

        @Test
        @Order(1)
        void firstFailure() {
            throw FIRST;
        }

        @Test
        @Order(2)
        void success() {}

        @Test
        @Order(3)
        void secondFailure() {
            throw SECOND;
        }
    }

    static class FailingNestedContainers {

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();
    }

    static class PassingNestedContainers {

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();
    }

    @Testcontainers
    @TestClassOrder(ClassOrderer.OrderAnnotation.class)
    static class NestedFailures {

        static final RuntimeException FAILURE = new RuntimeException("nested method failure");

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();

        @Nested
        @Order(1)
        class Failing extends FailingNestedContainers {

            @Test
            void failure() {
                throw FAILURE;
            }
        }

        @Nested
        @Order(2)
        class Passing extends PassingNestedContainers {

            @Test
            void success() {}
        }
    }

    @Testcontainers
    static class ClassFailure {

        static final RuntimeException CLASS_FAILURE = new RuntimeException("class failure");

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();

        @Test
        void failure() {
            throw new RuntimeException("method failure");
        }

        @AfterAll
        static void afterAll() {
            throw CLASS_FAILURE;
        }
    }

    @Testcontainers
    static class NestedClassFailure {

        static final RuntimeException FAILURE = new RuntimeException("nested class failure");

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class Failing {

            @Test
            void success() {}

            @AfterAll
            void afterAll() {
                throw FAILURE;
            }
        }
    }

    @Testcontainers
    static class Successful {

        @Container
        static final TestLifecycleAwareContainerMock SHARED = new TestLifecycleAwareContainerMock();

        @Test
        void success() {}
    }
}
