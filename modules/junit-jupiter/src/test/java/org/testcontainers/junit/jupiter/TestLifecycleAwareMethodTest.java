package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.testcontainers.junit.jupiter.TestLifecycleAwareContainerMock.AFTER_TEST;
import static org.testcontainers.junit.jupiter.TestLifecycleAwareContainerMock.BEFORE_TEST;

// The order of @ExtendsWith and @Testcontainers is crucial for the tests
@ExtendWith({TestLifecycleAwareMethodTest.SharedContainerAfterAllTestExtension.class})
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestLifecycleAwareMethodTest {
    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    @Container
    private static final TestLifecycleAwareContainerMock SHARED_CONTAINER = new TestLifecycleAwareContainerMock();

    private static TestLifecycleAwareContainerMock startedTestContainer;

    @BeforeAll
    static void beforeAll() {
        assertEquals(singletonList(BEFORE_TEST), SHARED_CONTAINER.getLifecycleMethodCalls());
    }

    @Test
    @Order(1)
    void should_prepare_before_and_after_test() {
        // we can only test for a call to afterTest() after this test has been finished.
        startedTestContainer = testContainer;
    }

    @Test
    @Order(2)
    void should_call_beforeTest_first_afterTest_later() {
        assertEquals(asList(BEFORE_TEST, AFTER_TEST), startedTestContainer.getLifecycleMethodCalls());
    }

    static class SharedContainerAfterAllTestExtension implements AfterAllCallback {
        // Unfortunately it's not possible to write a @Test that is run after all tests
        @Override
        public void afterAll(ExtensionContext context) {
            assertEquals(asList(BEFORE_TEST, AFTER_TEST), SHARED_CONTAINER.getLifecycleMethodCalls());
        }
    }
}
