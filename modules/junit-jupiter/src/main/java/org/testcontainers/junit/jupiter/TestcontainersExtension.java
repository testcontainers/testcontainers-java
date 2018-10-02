package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.Preconditions;
import org.testcontainers.lifecycle.Startable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class TestcontainersExtension implements TestInstancePostProcessor, BeforeEachCallback, AfterEachCallback {

    private static final Namespace NAMESPACE = Namespace.create(TestcontainersExtension.class);

    @Override
    public void postProcessTestInstance(final Object testInstance, final ExtensionContext context) {
        ExtensionContext.Store store = context.getStore(NAMESPACE);
        findSharedContainers(testInstance).forEach(container -> {
            StartableCloseableResourceAdapter started = store.getOrComputeIfAbsent(container.key, k ->
                container.start(), StartableCloseableResourceAdapter.class);
            setSharedContainerToField(testInstance, started.fieldName, started.container);
        });
    }

    private static void setSharedContainerToField(Object testInstance, String fieldName, Startable container) {
        try {
            Field sharedContainerField = testInstance.getClass().getDeclaredField(fieldName);
            sharedContainerField.setAccessible(true);
            sharedContainerField.set(testInstance, container);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExtensionConfigurationException("Can not set shared container instance to field " + fieldName);
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        findRestartedContainers(context.getRequiredTestInstance()).forEach(Startable::start);
    }

    @Override
    public void afterEach(final ExtensionContext context) {
        findRestartedContainers(context.getRequiredTestInstance()).forEach(Startable::stop);
    }

    private List<StartableCloseableResourceAdapter> findSharedContainers(Object testInstance) {
        return findAnnotatedContainers(testInstance, Shared.class).collect(toList());
    }

    private List<Startable> findRestartedContainers(Object testInstance) {
        return findAnnotatedContainers(testInstance, Restarted.class).map(s -> s.container).collect(toList());
    }

    private Stream<StartableCloseableResourceAdapter> findAnnotatedContainers(Object testInstance, Class<? extends Annotation> annotation) {
        return Arrays.stream(testInstance.getClass().getDeclaredFields())
            .filter(f -> Startable.class.isAssignableFrom(f.getType()))
            .filter(f -> AnnotationSupport.isAnnotated(f, annotation))
            .map(f -> getContainerInstance(testInstance, f));
    }

    private static StartableCloseableResourceAdapter getContainerInstance(final Object testInstance, final Field field) {
        try {
            field.setAccessible(true);
            Startable containerInstance = Preconditions.notNull((Startable) field.get(testInstance), "Container " + field.getName() + " needs to be initialized");
            return new StartableCloseableResourceAdapter(testInstance.getClass().getName(), field.getName(), containerInstance);
        } catch (IllegalAccessException e) {
            throw new ExtensionConfigurationException("Can not access container defined in field " + field.getName());
        }
    }

    private static class StartableCloseableResourceAdapter implements CloseableResource {

        private String key;

        private String fieldName;

        private Startable container;

        private StartableCloseableResourceAdapter(String className, String fieldName, Startable container) {
            this.key = className + "." + fieldName;
            this.fieldName = fieldName;
            this.container = container;
        }

        private StartableCloseableResourceAdapter start() {
            container.start();
            return this;
        }

        @Override
        public void close() throws Throwable {
            container.stop();
        }
    }
}
