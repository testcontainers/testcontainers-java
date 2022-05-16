package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.ComparableVersion;

import java.util.ArrayList;
import java.util.List;

public class PostgreSQLWaitStrategy extends AbstractWaitStrategy {

    private final String version;

    public PostgreSQLWaitStrategy(String version) {
        this.version = version;
    }

    @Override
    protected void waitUntilReady() {
        boolean isAtLeastMajorVersion94 = new ComparableVersion(this.version).isGreaterThanOrEqualTo("9.4");

        List<String> firstAttempt = new ArrayList<>();
        firstAttempt.add(".*PostgreSQL init process complete.*$");
        firstAttempt.add(".*database system is ready to accept connections.*$");

        List<String> secondAttempt = new ArrayList<>();
        if (isAtLeastMajorVersion94) {
            secondAttempt.add(".*PostgreSQL Database directory appears to contain a database.*$");
        }
        secondAttempt.add(".*database system is ready to accept connections.*$");

        new MultiLogMessageWaitStrategy()
            .withRegEx(firstAttempt)
            .withRegEx(secondAttempt)
            .waitUntilReady(this.waitStrategyTarget);
    }

}
