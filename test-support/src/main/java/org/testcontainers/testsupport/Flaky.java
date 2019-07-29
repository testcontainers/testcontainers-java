package org.testcontainers.testsupport;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * TODO: Javadocs
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface Flaky {

    String rationale();
}
