package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.testcontainers.lifecycle.Startable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Represents a container provider method annotated with {@link ContainerProvider}.
 * This class encapsulates the metadata and invocation logic for provider methods.
 */
class ProviderMethod {

    private final Method method;

    private final String name;

    private final ContainerProvider.Scope scope;

    private final Class<?> declaringClass;

    /**
     * Creates a new ProviderMethod instance.
     *
     * @param method the provider method
     * @param annotation the ContainerProvider annotation
     */
    ProviderMethod(Method method, ContainerProvider annotation) {
        this.method = method;
        this.name = annotation.name();
        this.scope = annotation.scope();
        this.declaringClass = method.getDeclaringClass();

        validateMethod();
    }

    /**
     * Validates that the provider method meets all requirements.
     *
     * @throws ExtensionConfigurationException if validation fails
     */
    private void validateMethod() {
        // Check return type
        if (!Startable.class.isAssignableFrom(method.getReturnType())) {
            throw new ExtensionConfigurationException(
                String.format(
                    "Container provider method '%s' in class '%s' must return a type that implements Startable, but returns %s",
                    method.getName(),
                    declaringClass.getName(),
                    method.getReturnType().getName()
                )
            );
        }

        // Check that method is not private
        if (Modifier.isPrivate(method.getModifiers())) {
            throw new ExtensionConfigurationException(
                String.format(
                    "Container provider method '%s' in class '%s' must not be private",
                    method.getName(),
                    declaringClass.getName()
                )
            );
        }

        // Check that method has no parameters
        if (method.getParameterCount() > 0) {
            throw new ExtensionConfigurationException(
                String.format(
                    "Container provider method '%s' in class '%s' must not have parameters",
                    method.getName(),
                    declaringClass.getName()
                )
            );
        }

        // Make method accessible
        method.setAccessible(true);
    }

    /**
     * Invokes the provider method to create a container instance.
     *
     * @param testInstance the test instance (null for static methods)
     * @return the created container
     * @throws ExtensionConfigurationException if invocation fails
     */
    public Startable invoke(Object testInstance) {
        try {
            Object result = method.invoke(testInstance);

            if (result == null) {
                throw new ExtensionConfigurationException(
                    String.format(
                        "Container provider method '%s' in class '%s' returned null",
                        method.getName(),
                        declaringClass.getName()
                    )
                );
            }

            return (Startable) result;
        } catch (IllegalAccessException e) {
            throw new ExtensionConfigurationException(
                String.format(
                    "Failed to access container provider method '%s' in class '%s'",
                    method.getName(),
                    declaringClass.getName()
                ),
                e
            );
        } catch (InvocationTargetException e) {
            throw new ExtensionConfigurationException(
                String.format(
                    "Container provider method '%s' in class '%s' threw an exception",
                    method.getName(),
                    declaringClass.getName()
                ),
                e.getCause()
            );
        }
    }

    /**
     * Returns the provider name.
     *
     * @return the provider name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the container scope.
     *
     * @return the scope
     */
    public ContainerProvider.Scope getScope() {
        return scope;
    }

    /**
     * Returns whether this is a static provider method.
     *
     * @return true if static, false otherwise
     */
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    /**
     * Returns the declaring class of this provider method.
     *
     * @return the declaring class
     */
    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    /**
     * Returns the underlying method.
     *
     * @return the method
     */
    public Method getMethod() {
        return method;
    }

    @Override
    public String toString() {
        return String.format(
            "ProviderMethod[name='%s', scope=%s, method=%s.%s()]",
            name,
            scope,
            declaringClass.getSimpleName(),
            method.getName()
        );
    }
}
