package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

@Testcontainers
class TestLifecycleAwareSignallingTest {

    @Container
    private final TestLifecycleAwareContainerMock testContainer = new TestLifecycleAwareContainerMock();

    @Test
    void should_call_beforeTest_on_TestLifecycleAware_container() {
        assertEquals(1, testContainer.getNumBeforeTestsCalls());
    }
}
