package org.testcontainers.utility;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicReference;

public class MockTestcontainersConfigurationExtension implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {
    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(MockTestcontainersConfigurationExtension.class);
    private static final AtomicReference<TestcontainersConfiguration> REF = TestcontainersConfiguration.getInstanceField();
    private static final String KEY = MockTestcontainersConfigurationExtension.class.getName() + ".PREV";

    @Override
    public void beforeAll(ExtensionContext context) {
        // Invoke configuration load
        TestcontainersConfiguration.getInstance();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        TestcontainersConfiguration previous = REF.get();
        REF.set(Mockito.spy(previous));
        context.getStore(NAMESPACE).put(KEY, previous);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        TestcontainersConfiguration previous = context.getStore(NAMESPACE).get(KEY, TestcontainersConfiguration.class);
        REF.set(previous);
    }
}
