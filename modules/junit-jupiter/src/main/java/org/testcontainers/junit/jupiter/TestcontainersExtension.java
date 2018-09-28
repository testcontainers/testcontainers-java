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
    public void beforeAll(final ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        for (final Field field : testClass.getDeclaredFields()) {
            if (Startable.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Startable container = Preconditions.notNull((Startable) field.get(testClass), "Container " + field.getName() + " needs to be initialized!");
                container.start();
            }
        }
    }

    @Override
    public void beforeEach(final ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (Startable.class.isAssignableFrom(field.getType()) && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Startable container = Preconditions.notNull((Startable) field.get(testInstance), "Container " + field.getName() + " needs to be initialized!");
                container.start();
            }
        }
    }

    @Override
    public void afterEach(final ExtensionContext context) throws Exception {
        Object testInstance = context.getRequiredTestInstance();
        for (Field field : testInstance.getClass().getDeclaredFields()) {
            if (Startable.class.isAssignableFrom(field.getType()) && !Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Startable container = Preconditions.notNull((Startable) field.get(testInstance), "Container " + field.getName() + " needs to be initialized!");
                container.stop();
            }
        }
    }

    @Override
    public void afterAll(final ExtensionContext context) throws Exception {
        Class<?> testClass = context.getRequiredTestClass();
        for (final Field field : testClass.getDeclaredFields()) {
            if (Startable.class.isAssignableFrom(field.getType()) && Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                Startable container = Preconditions.notNull((Startable) field.get(testClass), "Container " + field.getName() + " needs to be initialized!");
                container.stop();
            }
        }
    }

}
