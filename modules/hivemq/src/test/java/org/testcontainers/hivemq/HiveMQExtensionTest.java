package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Yannick Weber
 */
class HiveMQExtensionTest {

    @Test
    void builder_classDoesNotImplementExtensionMain_exception() {
        assertThrows(IllegalArgumentException.class, () -> HiveMQExtension.builder().mainClass(HiveMQExtensionTest.class));
    }
}
