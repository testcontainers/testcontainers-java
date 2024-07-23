package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code EnabledIfDockerAvailable} is a JUnit Jupiter extension to enable tests only if Docker is available.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(EnabledIfDockerAvailableCondition.class)
public @interface EnabledIfDockerAvailable {
}
