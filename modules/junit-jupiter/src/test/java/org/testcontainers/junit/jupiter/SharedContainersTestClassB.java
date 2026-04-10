package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Second of two test classes extending {@link SharedContainersBaseTest}.
 *
 * <p>Together with {@link SharedContainersTestClassA}, this verifies that {@link SharedContainers}
 * starts the container only once across test class boundaries.</p>
 */
class SharedContainersTestClassB extends SharedContainersBaseTest {

    @Test
    void container_is_running() {
        assertThat(SHARED.isRunning()).isTrue();
    }

    @Test
    void container_id_is_consistent_across_classes() {
        String currentId = SHARED.getContainerId();
        assertThat(currentId).isNotBlank();

        // If this is the first class to run, record the ID.
        // If another class already ran, verify we see the same container.
        if (!observedContainerId.compareAndSet(null, currentId)) {
            assertThat(currentId)
                .as("SharedContainers should reuse the same container across test classes")
                .isEqualTo(observedContainerId.get());
        }
    }
}
