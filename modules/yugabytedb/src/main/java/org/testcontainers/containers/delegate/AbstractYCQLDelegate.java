package org.testcontainers.containers.delegate;

import org.testcontainers.delegate.DatabaseDelegate;

/**
 * An abstract delegate do-nothing class
 */
public abstract class AbstractYCQLDelegate implements DatabaseDelegate {

    @Override
    public void execute(
        String statement,
        String scriptPath,
        int lineNumber,
        boolean continueOnError,
        boolean ignoreFailedDrops
    ) {
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }
}
