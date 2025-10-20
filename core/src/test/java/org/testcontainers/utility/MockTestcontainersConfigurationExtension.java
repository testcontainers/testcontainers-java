package org.testcontainers.utility;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

/**
 * This {@link org.junit.jupiter.api.extension.Extension} applies a spy on {@link TestcontainersConfiguration}
 * for testing features that depend on the global configuration.
 */
public class MockTestcontainersConfigurationExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NS = ExtensionContext.Namespace.create(
        MockTestcontainersConfigurationExtension.class
    );

    static AtomicReference<TestcontainersConfiguration> REF = TestcontainersConfiguration.getInstanceField();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        TestcontainersConfiguration previous = REF.get();
        if (previous == null) {
            previous = TestcontainersConfiguration.getInstance();
        }
        REF.set(Mockito.spy(previous));
        context.getStore(NS).put(context.getUniqueId(), previous);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        TestcontainersConfiguration previous = context
            .getStore(NS)
            .remove(context.getUniqueId(), TestcontainersConfiguration.class);
        REF.set(previous);
    }
}
