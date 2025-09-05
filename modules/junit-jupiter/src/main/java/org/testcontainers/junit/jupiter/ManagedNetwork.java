package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.AutoClose;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code @Network} annotation is used in conjunction with the {@link Testcontainers} annotation
 * to mark networks that should be managed by the Testcontainers extension.
 *
 * @see Testcontainers
 */
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@AutoClose
public @interface ManagedNetwork {
}
