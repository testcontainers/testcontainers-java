package org.testcontainers.spock

import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * {@code @Testcontainers} is a Spock extension to activate automatic
 * startup and stop of containers used in a test case.
 *
 * <p>The Testcontainers extension finds all fields that extend
 * {@link org.testcontainers.containers.GenericContainer} or
 * {@link org.testcontainers.containers.DockerComposeContainer} and calls their
 * container lifecycle methods. Containers annotated with {@link spock.lang.Shared}
 * will be shared between test methods. They will be
 * started only once before any test method is executed and stopped after the
 * last test method has executed. Containers without {@link spock.lang.Shared}
 * annotation will be started and stopped for every test method.</p>
 *
 * <p>The annotation {@code @Testcontainers} can be used on a superclass in
 * the test hierarchy as well. All subclasses will automatically inherit
 * support for the extension.</p>
 *
 * <p>Example:</p>
 *
 * <pre>
 * &#64;Testcontainers
 * class MyTestcontainersTests extends Specification {
 *
 *     // will be started only once in setupSpec() and stopped after last test method
 *     &#64;Shared
 *     MySQLContainer MY_SQL_CONTAINER = new MySQLContainer()
 *
 *     // will be started before and stopped after each test method
 *     PostgreSQLContainer postgresqlContainer = new PostgreSQLContainer()
 *             .withDatabaseName('foo')
 *             .withUsername('foo')
 *             .withPassword('secret')
 *
 *     def 'test'() {
 *         expect:
 *         MY_SQL_CONTAINER.running
 *         postgresqlContainer.running
 *     }
 * }
 * </pre>
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@ExtensionAnnotation(TestcontainersExtension)
@interface Testcontainers {
}
