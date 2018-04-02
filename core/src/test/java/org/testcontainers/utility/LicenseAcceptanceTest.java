package org.testcontainers.utility;

import org.junit.Test;

public class LicenseAcceptanceTest {

    @Test
    public void testForExistingNames() {
        LicenseAcceptance.assertLicenseAccepted("a");
        LicenseAcceptance.assertLicenseAccepted("b");
    }

    @Test(expected = IllegalStateException.class)
    public void testForMissingNames() {
        LicenseAcceptance.assertLicenseAccepted("c");
    }
}
