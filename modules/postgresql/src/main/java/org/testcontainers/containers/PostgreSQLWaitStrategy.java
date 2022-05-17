package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.utility.ComparableVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostgreSQLWaitStrategy extends AbstractWaitStrategy {

    private final Pattern pattern = Pattern.compile("(?s)(?:\\d\\S*)");

    @Override
    protected void waitUntilReady() {
        try {
            String postgresVersion = this.waitStrategyTarget.execInContainer("postgres", "-V").getStdout();
            Matcher matcher = this.pattern.matcher(postgresVersion);
            if (matcher.find()) {
                String version = matcher.group();
                boolean isAtLeastMajorVersion94 = new ComparableVersion(version).isGreaterThanOrEqualTo("9.4");

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
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
