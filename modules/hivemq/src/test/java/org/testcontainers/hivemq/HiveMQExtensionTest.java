package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HiveMQExtensionTest {

    @Test
    void builder_classDoesNotImplementExtensionMain_exception() {
        assertThatExceptionOfType(IllegalArgumentException.class)
            .isThrownBy(() -> HiveMQExtension.builder().mainClass(HiveMQExtensionTest.class));
    }
}
