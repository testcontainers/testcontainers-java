package org.testcontainers.utility;

import org.jetbrains.annotations.NotNull;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This {@link TestRule} applies a spy on {@link TestcontainersConfiguration}
 * for testing features that depend on the global configuration.
 */
public class MockTestcontainersConfigurationRule implements TestRule {

    static AtomicReference<TestcontainersConfiguration> REF = TestcontainersConfiguration.getInstanceField();

    @NotNull
    @Override
    public Statement apply(@NotNull Statement base, @NotNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                TestcontainersConfiguration previous = REF.get();
                if (previous == null) {
                    previous = TestcontainersConfiguration.getInstance();
                }
                REF.set(Mockito.spy(previous));

                try {
                    base.evaluate();
                } finally {
                    REF.set(previous);
                }
            }
        };
    }
}
