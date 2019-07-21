package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.Assert.assertEquals;

@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
class TestLifecycleAwareSignallingTest {

    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    static private TestLifecycleAwareContainerMock startedTestContainer;

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
}
