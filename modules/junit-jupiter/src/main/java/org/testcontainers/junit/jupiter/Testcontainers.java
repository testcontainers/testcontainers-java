package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Testcontainers} is a JUnit Jupiter extension to activate automatic
 * startup and stop of containers used in a test case.
 *
 * <p>The test containers extension finds all fields of type
 * {@link org.testcontainers.lifecycle.Startable} and calls their container
 * lifecylce methods. Containers declared as static fields will be shared
 * between test methods. They will be started only once before any test method
 * is executed and stopped after the last test method has executed. Containers
 * declared as instance fields will be started and stopped for every test
 * method.</p>
 *
 * <p>Example:
 *
 * <pre>
 * &#64;Testcontainers
 * class MyTestcontainersTests {
 *
 *     // will be shared between test methods
 *     private static final MySQLContainer MY_SQL_CONTAINER = new MySQLContainer();
 *
 *     // will be started before and stopped after each test method
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
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(TestcontainersExtension.class)
public @interface Testcontainers {
}
