package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @SharedContainers} is a JUnit Jupiter extension to activate automatic startup of
 * containers that are shared across multiple test classes.
 *
 * <p>Unlike {@link Testcontainers}, which manages container lifecycle per test class,
 * {@code @SharedContainers} uses the root {@link org.junit.jupiter.api.extension.ExtensionContext}
 * store so that containers declared as {@code static} fields annotated with {@link Container} are
 * started only once for the entire test suite (JVM session) and stopped automatically when the
 * JVM exits (via Ryuk or JVM shutdown).</p>
 *
 * <p>This is the recommended approach when multiple test classes extend a common base class and
 * need to share the same container instances. Using {@link Testcontainers} in this scenario causes
 * containers to be restarted for each test class.</p>
 *
 * <p>Containers declared as instance fields will still be started and stopped for every test
 * method, just like with {@link Testcontainers}.</p>
 *
 * <p>The annotation {@code @SharedContainers} can be used on a superclass in the test hierarchy.
 * All subclasses will automatically inherit support for the extension.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 * &#64;SharedContainers
 * abstract class AbstractIntegrationTest {
 *
 *     // started once for the entire test suite, shared across all subclasses
 *     &#64;Container
 *     static final MySQLContainer&lt;?&gt; MY_SQL = new MySQLContainer&lt;&gt;("mysql:8");
 * }
 *
 * class UserServiceTest extends AbstractIntegrationTest {
 *     &#64;Test
 *     void test() {
 *         assertTrue(MY_SQL.isRunning());
 *     }
 * }
 *
 * class OrderServiceTest extends AbstractIntegrationTest {
 *     &#64;Test
 *     void test() {
 *         // same MY_SQL instance as UserServiceTest
 *         assertTrue(MY_SQL.isRunning());
 *     }
 * }
 * </pre>
 *
 * @see Container
 * @see Testcontainers
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(SharedContainersExtension.class)
@Inherited
public @interface SharedContainers {
    /**
     * Whether tests should be disabled (rather than failing) when Docker is not available.
     * Defaults to {@code false}.
     *
     * @return if the tests should be disabled when Docker is not available
     */
    boolean disabledWithoutDocker() default false;

    /**
     * Whether containers should start in parallel. Defaults to {@code false}.
     *
     * @return if the containers should start in parallel
     */
    boolean parallel() default false;
}
