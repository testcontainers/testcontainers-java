package org.testcontainers.testsupport;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspired by {@link org.junit.runners.model.MultipleFailureException}
 */
public class FlakyTestException extends IllegalStateException {
    
    private static final long serialVersionUID = 1L;

    private final List<Throwable> errors;

    public FlakyTestException(String message, List<Throwable> errors) {
        this.errors = new ArrayList<>(errors);
    }
    
    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder(String.format("There were %d errors:", errors.size()));
        for (Throwable e : errors) {
            sb.append(String.format("%n  %s(%s)", e.getClass().getName(), e.getMessage()));
        }
        return sb.toString();
    }

    @Override
    public void printStackTrace() {
        for (Throwable e: errors) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void printStackTrace(PrintStream s) {
        for (Throwable e: errors) {
            e.printStackTrace(s);
        }
    }
    
    @Override
    public void printStackTrace(PrintWriter s) {
        for (Throwable e: errors) {
            e.printStackTrace(s);
        }
    }
}
