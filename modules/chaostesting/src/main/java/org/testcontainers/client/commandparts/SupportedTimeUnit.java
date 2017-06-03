package org.testcontainers.client.commandparts;

/**
 * Created by novy on 01.01.17.
 */
public enum SupportedTimeUnit {
    MILLISECONDS {
        @Override
        String abbreviation() {
            return "ms";
        }

        @Override
        long millisecondsMultiplier() {
            return 1;
        }
    }, SECONDS {
        @Override
        String abbreviation() {
            return "s";
        }

        @Override
        long millisecondsMultiplier() {
            return 1000;
        }
    }, MINUTES {
        @Override
        String abbreviation() {
            return "m";
        }

        @Override
        long millisecondsMultiplier() {
            return 60_000;
        }
    }, HOURS {
        @Override
        String abbreviation() {
            return "h";
        }

        @Override
        long millisecondsMultiplier() {
            return 3_600_000;
        }
    };

    abstract String abbreviation();

    abstract long millisecondsMultiplier();
}
