package org.testcontainers.utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LicenseAcceptanceTest {

    @Test
    public void testForExistingNames() {
        LicenseAcceptance.assertLicenseAccepted("a");
        LicenseAcceptance.assertLicenseAccepted("b");
    }

    @Test
    public void testForMissingNames() {
        assertThatThrownBy(() -> LicenseAcceptance.assertLicenseAccepted("c"))
            .isInstanceOf(IllegalStateException.class);
    }
}
