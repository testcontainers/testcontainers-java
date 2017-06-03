package org.testcontainers.junit.assumptions;

import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.junit.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;

/**
 * TODO: Javadocs
 */
@Slf4j
public class DockerAvailableAssumptionRule implements TestRule {

    private static Boolean previousOutcomes;

    DockerAvailableAssumptionRule() {
    }

    @Override
    @Synchronized
    public Statement apply(Statement base, Description description) {

        if (previousOutcomes == null) {
            try {
                log.info("Checking for presence of a working Docker environment");
                DockerClientFactory.instance().client();
                log.info("Docker environment found");
                previousOutcomes = true;
            } catch (Exception ignored) {
                previousOutcomes = false;
            }
        }

        if (previousOutcomes) {
            return base;
        } else {
            throw new AssumptionViolatedException("No docker environment found");
        }
    }
}
