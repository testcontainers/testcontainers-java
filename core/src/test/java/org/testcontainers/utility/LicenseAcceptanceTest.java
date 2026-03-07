package org.testcontainers.utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicenseAcceptanceTest {

    @Test
    void testForExistingNames() {
        LicenseAcceptance.assertLicenseAccepted("a");
        LicenseAcceptance.assertLicenseAccepted("b");
    }

    @Test
    void testForMissingNames() {
        assertThatThrownBy(() -> LicenseAcceptance.assertLicenseAccepted("c"))
            .isInstanceOf(IllegalStateException.class);
    }
}
