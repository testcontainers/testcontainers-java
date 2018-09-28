package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.util.Preconditions;
import org.testcontainers.lifecycle.Startable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class TestcontainersExtension implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws IllegalAccessException {
        Class<?> testClass = context.getRequiredTestClass();
        for (final Field field : testClass.getDeclaredFields()) {
            if (isSharedContainer(field)) {
                startContainer(testClass, field);
            }
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws IllegalAccessException {
        Object testInstance = context.getRequiredTestInstance();
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (isRestartContainer(field)) {
                startContainer(testInstance, field);
            }
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) throws IllegalAccessException {
        Object testInstance = context.getRequiredTestInstance();
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (isRestartContainer(field)) {
                stopContainer(testInstance, field);
            }
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) throws IllegalAccessException {
        Class<?> testClass = context.getRequiredTestClass();
        for (final Field field : testClass.getDeclaredFields()) {
            if (isSharedContainer(field)) {
                stopContainer(testClass, field);
            }
        }
    }

    private static boolean isSharedContainer(final Field field) {
        return Startable.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers());
    }

    private static boolean isRestartContainer(final Field field) {
        return Startable.class.isAssignableFrom(field.getType()) && !Modifier.isStatic(field.getModifiers());
    }

    private static void startContainer(final Object fieldOwner, final Field field) throws IllegalAccessException {
        field.setAccessible(true);
        Startable container = Preconditions.notNull((Startable) field.get(fieldOwner), "Container " + field.getName() + " needs to be initialized!");
        container.start();
    }

    private static void stopContainer(final Object fieldOwner, final Field field) throws IllegalAccessException {
        field.setAccessible(true);
        Startable container = Preconditions.notNull((Startable) field.get(fieldOwner), "Container " + field.getName() + " needs to be initialized!");
        container.stop();
    }

}
