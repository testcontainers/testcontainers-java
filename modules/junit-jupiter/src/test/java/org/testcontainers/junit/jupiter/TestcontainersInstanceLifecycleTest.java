package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.testcontainers.lifecycle.Startable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that instance {@link Container @Container} fields are started exactly once
 * per test instance for both {@link TestInstance.Lifecycle#PER_CLASS} and
 * {@link TestInstance.Lifecycle#PER_METHOD} lifecycles.
 */
@Testcontainers
class TestcontainersInstanceLifecycleTest {

    @Container
    private static final StartCountingMock staticContainer = new StartCountingMock();

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PerClass {

        @Container
        private final StartCountingMock instanceContainer = new StartCountingMock();

        @Test
        @Order(1)
        void first_test() {
            assertThat(staticContainer.starts).isEqualTo(1);
            assertThat(instanceContainer.starts).isEqualTo(1);
        }

        @Test
        @Order(2)
        void second_test() {
            assertThat(staticContainer.starts).as("Static container should be started exactly once").isEqualTo(1);
            assertThat(instanceContainer.starts)
                .as("PER_CLASS instance container should be started for every test")
                .isEqualTo(2);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class PerMethod {

        @Container
        private final StartCountingMock instanceContainer = new StartCountingMock();

        @Test
        @Order(1)
        void first_test() {
            assertThat(staticContainer.starts).isEqualTo(1);
            assertThat(instanceContainer.starts).isEqualTo(1);
        }

        @Test
        @Order(2)
        void second_test() {
            assertThat(staticContainer.starts).as("Static container should be started exactly once").isEqualTo(1);
            assertThat(instanceContainer.starts).isEqualTo(1);
        }
    }

    static class StartCountingMock implements Startable {

        int starts;

        @Override
        public void start() {
            starts++;
        }

        @Override
        public void stop() {}
    }
}
