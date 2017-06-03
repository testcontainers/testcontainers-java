package org.testcontainers.junit.assumptions;

import lombok.experimental.UtilityClass;

/**
 * Rule that
 */
@UtilityClass
public class Assumptions {

    public static DockerAvailableAssumptionRule assumeDockerPresent() {
        return new DockerAvailableAssumptionRule();
    }

}
