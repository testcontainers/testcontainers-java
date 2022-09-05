package org.testcontainers.junit4;

import org.junit.validator.ValidateWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code @Container} annotation is used in conjunction with the {@link TestContainersRunner} Junit Runner
 *
 * @see org.junit.runner.RunWith
 * @see TestContainersRunner
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@ValidateWith(ContainerValidator.class)
public @interface Container {
}
