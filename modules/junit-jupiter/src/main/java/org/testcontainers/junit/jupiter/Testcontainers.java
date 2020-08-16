package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Testcontainers} is a JUnit Jupiter extension to activate automatic
 * startup and stop of containers used in a test case.
 *
 * <p>The test containers extension finds all fields that are annotated with
 * {@link Container} and calls their container lifecycle methods. Containers
 * declared as static fields will be shared between test methods. They will be
 * started only once before any test method is executed and stopped after the
 * last test method has executed. Containers declared as instance fields will
 * be started and stopped for every test method.</p>
 *
 * <p>The annotation {@code @Testcontainers} can be used on a superclass in
 * the test hierarchy as well. All subclasses will automatically inherit
 * support for the extension.</p>
 *
 * <p><strong>Note:</strong> This extension has only been tested with sequential
 * test execution. Using it with parallel test execution is unsupported and
 * may have unintended side effects.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 * &#64;Testcontainers
 * class MyTestcontainersTests {
 *
 *     // will be shared between test methods
 *     &#64;Container
 *     private static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer();
 *
 *     // will be started before and stopped after each test method
 *     &#64;Container
 *     private PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer()
 *             .withDatabaseName("foo")
 *             .withUsername("foo")
 *             .withPassword("secret");
 *
 *     &#64;Test
 *     void test() {
 *         assertTrue(MY_SQL_CONTAINER.isRunning());
 *         assertTrue(postgresqlContainer.isRunning());
 *     }
 * }
 * </pre>
 *
 * @see Container
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestcontainersExtension.class)
@Inherited
public @interface Testcontainers {

    /**
     * Whether tests should be disabled (rather than failing) when Docker is not available.
     */
    boolean disabledWithoutDocker() default false;
}
