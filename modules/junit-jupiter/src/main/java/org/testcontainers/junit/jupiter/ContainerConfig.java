package org.testcontainers.junit.jupiter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @ContainerConfig} is used to declare that a test method or test class
 * requires a container provided by a {@link ContainerProvider}.
 *
 * <p>The annotation references a container by name and controls whether to reuse
 * an existing instance or create a new one.</p>
 *
 * <p>When applied to a test method, the container will be available during that test.
 * When applied to a test class, the container will be available for all test methods
 * in that class.</p>
 *
 * <p>Example:</p>
 * <pre>
 * &#64;Test
 * &#64;ContainerConfig(name = "redis", needNewInstance = false)
 * void testCaching() {
 *     // Redis container is available and started
 * }
 * </pre>
 *
 * @see ContainerProvider
 * @see Testcontainers
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ContainerConfig {

    /**
     * The name of the container provider to use.
     * This must match the name specified in a {@link ContainerProvider} annotation.
     *
     * @return the container provider name
     */
    String name();

    /**
     * Whether to create a new container instance for this test.
     *
     * <p>If {@code false} (default), the container instance will be reused according
     * to the provider's scope. Multiple tests referencing the same provider will share
     * the same container instance.</p>
     *
     * <p>If {@code true}, a new container instance will be created for this test,
     * started before the test, and stopped after the test completes. This provides
     * test isolation at the cost of slower execution.</p>
     *
     * @return {@code true} to create a new instance, {@code false} to reuse
     */
    boolean needNewInstance() default false;

    /**
     * Whether to inject the container as a test method parameter.
     *
     * <p>When {@code true}, the container can be injected as a parameter to the test method.
     * The parameter type must be compatible with the container type returned by the provider.</p>
     *
     * <p>Example:</p>
     * <pre>
     * &#64;Test
     * &#64;ContainerConfig(name = "redis", injectAsParameter = true)
     * void testWithInjection(GenericContainer&lt;?&gt; redis) {
     *     String host = redis.getHost();
     * }
     * </pre>
     *
     * @return {@code true} to enable parameter injection, {@code false} otherwise
     */
    boolean injectAsParameter() default false;
}
