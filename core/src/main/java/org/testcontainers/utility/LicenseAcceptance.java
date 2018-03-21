package org.testcontainers.utility;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.experimental.UtilityClass;

import java.net.URL;
import java.util.List;

/**
 * Utility class to ensure that licenses have been accepted by the developer.
 */
@UtilityClass
public class LicenseAcceptance {

    private static final String ACCEPTANCE_FILE_NAME = "container-license-acceptance.txt";

    public static void assertLicenseAccepted(final String imageName) {
        try {
            final URL url = Resources.getResource(ACCEPTANCE_FILE_NAME);
            final List<String> acceptedLicences = Resources.readLines(url, Charsets.UTF_8);

            if (acceptedLicences.stream().map(String::trim).anyMatch(imageName::equals)) {
                return;
            }
        } catch (Exception ignored) {
            // suppressed
        }

        throw new IllegalStateException("The image " + imageName + " requires you to accept a license agreement. " +
                        "Please place a file at the root of the classpath named " + ACCEPTANCE_FILE_NAME + ", e.g. at " +
                        "src/test/resources/" + ACCEPTANCE_FILE_NAME + ". This file should contain the line:\n  " +
                        imageName);

    }
}
