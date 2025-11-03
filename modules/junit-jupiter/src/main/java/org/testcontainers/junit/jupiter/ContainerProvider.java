package org.testcontainers.junit.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ContainerProvider} is used to mark methods that provide container instances
 * that can be referenced by name and reused across multiple tests.
 *
 * <p>Provider methods must be non-private, return a type that implements {@link org.testcontainers.lifecycle.Startable},
 * and can be either static or instance methods.</p>
 *
 * <p>Example:</p>
 * <pre>
 * &#64;ContainerProvider(name = "redis", scope = Scope.GLOBAL)
 * public GenericContainer&lt;?&gt; createRedis() {
 *     return new GenericContainer&lt;&gt;("redis:6.2")
 *         .withExposedPorts(6379);
 * }
 * </pre>
 *
 * @see ContainerConfig
 * @see Testcontainers
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerProvider {

    /**
     * The unique name identifying this container provider.
     * This name is used to reference the container in {@link ContainerConfig} annotations.
     *
     * @return the container provider name
     */
    String name();

    /**
     * The scope of the container lifecycle.
     *
     * <p>{@link Scope#CLASS} means the container is shared within a single test class
     * and will be stopped after all tests in that class complete.</p>
     *
     * <p>{@link Scope#GLOBAL} means the container is shared across all test classes
     * and will be stopped at the end of the test suite by the Ryuk container.</p>
     *
     * @return the container scope
     */
    Scope scope() default Scope.GLOBAL;

    /**
     * Defines the lifecycle scope of a container.
     */
    enum Scope {
        /**
         * Container is shared within a single test class.
         * The container will be started once for the class and stopped after all tests complete.
         */
        CLASS,

        /**
         * Container is shared across all test classes in the test suite.
         * The container will be started once and stopped at the end of the test suite.
         */
        GLOBAL
    }
}
