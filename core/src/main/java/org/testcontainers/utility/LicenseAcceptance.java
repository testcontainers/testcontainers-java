package org.testcontainers.utility;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import lombok.experimental.UtilityClass;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to ensure that licenses have been accepted by the developer.
 */
@UtilityClass
public class LicenseAcceptance {

    private final String ACCEPTANCE_FILE_NAME = "container-license-acceptance.txt";

    final Set<String> ACCEPTED_IMAGE_NAMES = new HashSet<>();

    public void assertLicenseAccepted(final String imageName) {
        if (ACCEPTED_IMAGE_NAMES.contains(imageName)) {
            return;
        }
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
