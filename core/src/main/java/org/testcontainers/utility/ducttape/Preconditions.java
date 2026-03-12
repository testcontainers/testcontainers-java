package org.testcontainers.utility.ducttape;

/**
 * Simple Preconditions check implementation.
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
public class Preconditions {

    /**
     * Check that a given condition is true. Will throw an IllegalArgumentException otherwise.
     * @param message message to display if the precondition check fails
     * @param condition the result of evaluating the condition
     */
    public static void check(String message, boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException("Precondition failed: " + message);
        }
    }
}
