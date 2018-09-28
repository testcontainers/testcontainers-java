package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.Preconditions;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace;

class TestcontainersExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    private static final Namespace NAMESPACE = Namespace.create(TestcontainersExtension.class);

    private static final String SHARED_CONTAINERS_STARTED_KEY = "shared_containers_started";

    private static final String CALLED_FIRST_TIME = "called_first_time";

    private static final String CONTAINERS = "containers";

    private static final String SHARED_CONTAINERS = "shared_containers";

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        context.getStore(NAMESPACE).put(SHARED_CONTAINERS, new HashSet<>());
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        Set<GenericContainer<?>> containers = new HashSet<>();
        Set<GenericContainer<?>> sharedContainers = (Set<GenericContainer<?>>) context.getStore(NAMESPACE).get(SHARED_CONTAINERS);
        if(context.getStore(NAMESPACE).get(CALLED_FIRST_TIME, Boolean.class) == null) {
            for (Field field : testInstance.getClass().getDeclaredFields()) {
                if (GenericContainer.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    GenericContainer<?> container = (GenericContainer<?>) field.get(testInstance);
                    Preconditions.notNull(container, "Container " + field.getName() + " needs to be initialized!");
                    if (AnnotationSupport.isAnnotated(field, Shared.class)) {
                        sharedContainers.add(container);
                    } else {
                        containers.add(container);
                    }
                }
            }
            context.getStore(NAMESPACE).put(CONTAINERS, containers);
            context.getStore(NAMESPACE).put(SHARED_CONTAINERS, sharedContainers);
            context.getStore(NAMESPACE).put(CALLED_FIRST_TIME, true);
        }

        if (context.getStore(NAMESPACE).get(SHARED_CONTAINERS_STARTED_KEY, Boolean.class) == null) {
            sharedContainers = (Set<GenericContainer<?>>) context.getStore(NAMESPACE).get(SHARED_CONTAINERS);
            sharedContainers.forEach(GenericContainer::start);
        }
        containers = (Set<GenericContainer<?>>) context.getStore(NAMESPACE).get(CONTAINERS);
        containers.forEach(GenericContainer::start);
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        Set<GenericContainer<?>> containers = (Set<GenericContainer<?>>) context.getStore(NAMESPACE).get(CONTAINERS);
        containers.forEach(GenericContainer::stop);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        Set<GenericContainer<?>> sharedContainers = (Set<GenericContainer<?>>) context.getStore(NAMESPACE).get(SHARED_CONTAINERS);
        sharedContainers.forEach(GenericContainer::stop);
    }

}
