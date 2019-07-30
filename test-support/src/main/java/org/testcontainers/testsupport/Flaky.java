package org.testcontainers.testsupport;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * TODO: Javadocs
 */
@Retention(RUNTIME)
@Target({METHOD})
public @interface Flaky {

    /**
     * @return a URL for a GitHub issue where this flaky test can be discussed, and where actions to resolve it can be
     * coordinated.
     */
    String githubIssueUrl();

    /**
     * @return a date at which this should be reviewed, in {@link java.time.format.DateTimeFormatter#ISO_LOCAL_DATE}
     * format (e.g. {@code 2020-12-03}). Now + 3 months is suggested. Once this date has passed, the annotation will
     * stop having an effect.
     */
    String reviewDate();
}
