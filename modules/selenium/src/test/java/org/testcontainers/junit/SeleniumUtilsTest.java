package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.SeleniumUtils;

import java.io.IOException;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by Julien LAMY
 */
class SeleniumUtilsTest {

    @Test
    void detectSeleniumVersionUnder3() throws IOException {
        checkSeleniumVersionDetected("manifests/MANIFEST-2.45.0.MF", "2.45.0");
    }

    @Test
    void detectSeleniumVersionUpper3() throws IOException {
        checkSeleniumVersionDetected("manifests/MANIFEST-3.5.2.MF", "3.5.2");
    }

    /**
     * Check if Selenium Version detected is the correct one.
     * @param urlManifest : manifest file
     * @throws IOException
     */
    private void checkSeleniumVersionDetected(String urlManifest, String expectedVersion) throws IOException {
        Manifest manifest = new Manifest();
        manifest.read(this.getClass().getClassLoader().getResourceAsStream(urlManifest));
        String seleniumVersion = SeleniumUtils.getSeleniumVersionFromManifest(manifest);
        assertThat(seleniumVersion)
            .as("Check if Selenium Version detected is the correct one.")
            .isEqualTo(expectedVersion);
    }
}
