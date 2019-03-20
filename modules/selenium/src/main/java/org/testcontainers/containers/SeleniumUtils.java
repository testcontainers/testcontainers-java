package org.testcontainers.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * Utility methods for Selenium.
 */
public final class SeleniumUtils {

    public static final String DEFAULT_SELENIUM_VERSION = "2.45.0";

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumUtils.class);

    private SeleniumUtils() {}

    /**
     * Based on the JARs detected on the classpath, determine which version of selenium-api is available.
     * @return the detected version of Selenium API, or DEFAULT_SELENIUM_VERSION if it could not be determined
     */
    public static String determineClasspathSeleniumVersion() {
        Set<String> seleniumVersions = new HashSet<>();
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<URL> manifests = classLoader.getResources("META-INF/MANIFEST.MF");

            while (manifests.hasMoreElements()) {
                URL manifestURL = manifests.nextElement();
                try (InputStream is = manifestURL.openStream()) {
                    Manifest manifest = new Manifest();
                    manifest.read(is);

                    String seleniumVersion = getSeleniumVersionFromManifest(manifest);
                    if (seleniumVersion != null) {
                        seleniumVersions.add(seleniumVersion);
                        LOGGER.info("Selenium API version {} detected on classpath", seleniumVersion);
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.debug("Failed to determine Selenium-Version from selenium-api JAR Manifest", e);
        }

        if (seleniumVersions.size() == 0) {
            LOGGER.warn("Failed to determine Selenium version from classpath - will use default version of {}", DEFAULT_SELENIUM_VERSION);
            return DEFAULT_SELENIUM_VERSION;
        }

        String foundVersion = seleniumVersions.iterator().next();
        if (seleniumVersions.size() > 1) {
            LOGGER.warn("Multiple versions of Selenium API found on classpath - will select {}, but this may not be reliable", foundVersion);
        }

        return foundVersion;
    }

    /**
     * Read Manifest to get Selenium Version.
     * @param manifest manifest
     * @return Selenium Version detected
     */
    public static String getSeleniumVersionFromManifest(Manifest manifest) {
        String seleniumVersion = null;
        Attributes buildInfo = manifest.getAttributes("Build-Info");
        if (buildInfo != null) {
            seleniumVersion = buildInfo.getValue("Selenium-Version");
        }

        // Compatibility Selenium > 3.X
        if(seleniumVersion == null) {
            Attributes seleniumInfo = manifest.getAttributes("Selenium");
            if (seleniumInfo != null) {
                seleniumVersion = seleniumInfo.getValue("Selenium-Version");
            }
        }
        return seleniumVersion;
    }
}
