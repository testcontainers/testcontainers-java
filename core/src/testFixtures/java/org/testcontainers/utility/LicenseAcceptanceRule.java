package org.testcontainers.utility;

import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class LicenseAcceptanceRule implements TestRule {

    @NotNull
    @Override
    public Statement apply(@NotNull Statement base, @NotNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    LicenseAcceptance.ACCEPTED_IMAGE_NAMES.clear();
                }
            }
        };
    }

    public void acceptLicense(String imageName) {
        LicenseAcceptance.ACCEPTED_IMAGE_NAMES.add(imageName);
    }
}
