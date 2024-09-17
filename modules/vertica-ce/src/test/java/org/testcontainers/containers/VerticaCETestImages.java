package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Vertica docker images
 * <p>
 * Vertica has three editions: Community, Express, and Premium, plus a few additional products.
 * <p>
 * The only edition available at docker hub is the Community Edition.
 */
public interface VerticaCETestImages {
    DockerImageName VERTICA_CE_TEST_IMAGE = DockerImageName.parse("vertica/vertica-ce:24.1.0-0");
}
