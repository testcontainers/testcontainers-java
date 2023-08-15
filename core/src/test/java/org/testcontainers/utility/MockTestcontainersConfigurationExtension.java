package org.testcontainers.utility;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.TestRule;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

/**TODO
 * This {@link TestRule} applies a spy on {@link TestcontainersConfiguration}
 * for testing features that depend on the global configuration.
 */
public class MockTestcontainersConfigurationExtension implements BeforeEachCallback, AfterEachCallback {

    static AtomicReference<TestcontainersConfiguration> REF = TestcontainersConfiguration.getInstanceField();

    private TestcontainersConfiguration previous;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        previous = REF.get();
        if (previous == null) {
            previous = TestcontainersConfiguration.getInstance();
        }
        REF.set(Mockito.spy(previous));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
        REF.set(previous);
    }
}
