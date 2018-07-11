package org.testcontainers.junit;

import org.junit.Test;
import org.testcontainers.containers.SeleniumUtils;

import java.io.IOException;
import java.util.jar.Manifest;

import static org.rnorth.visibleassertions.VisibleAssertions.*;


/**
 * Created by Julien LAMY
 */
public class SeleniumUtilsTest {

    @Test
    public void detectSeleniumVersionUnder3() throws IOException {
        checkSeleniumVersionDetected("manifests/MANIFEST-2.45.0.MF", "2.45.0");
    }

    @Test
    public void detectSeleniumVersionUpper3() throws IOException {
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
        assertEquals("Check if Selenium Version detected is the correct one.", expectedVersion, seleniumVersion);
    }

}
