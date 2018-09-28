package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.Preconditions;
import org.testcontainers.lifecycle.Startable;

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
        Set<Startable> containers = new HashSet<>();
        Set<Startable> sharedContainers = (Set<Startable>) context.getStore(NAMESPACE).get(SHARED_CONTAINERS);
        if(context.getStore(NAMESPACE).get(CALLED_FIRST_TIME, Boolean.class) == null) {
            for (Field field : testInstance.getClass().getDeclaredFields()) {
                if (Startable.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Startable container = (Startable) field.get(testInstance);
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
            sharedContainers = (Set<Startable>) context.getStore(NAMESPACE).get(SHARED_CONTAINERS);
            sharedContainers.forEach(Startable::start);
        }
        containers = (Set<Startable>) context.getStore(NAMESPACE).get(CONTAINERS);
        containers.forEach(Startable::start);
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        Set<Startable> containers = (Set<Startable>) context.getStore(NAMESPACE).get(CONTAINERS);
        containers.forEach(Startable::stop);
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        Set<Startable> sharedContainers = (Set<Startable>) context.getStore(NAMESPACE).get(SHARED_CONTAINERS);
        sharedContainers.forEach(Startable::stop);
    }

}
