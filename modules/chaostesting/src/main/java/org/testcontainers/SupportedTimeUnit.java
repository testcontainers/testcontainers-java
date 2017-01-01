package org.testcontainers;

/**
 * Created by novy on 01.01.17.
 */
public enum SupportedTimeUnit {
    MILLISECONDS {
        @Override
        String abbreviation() {
            return "ms";
        }
    }, SECONDS {
        @Override
        String abbreviation() {
            return "s";
        }
    }, MINUTES {
        @Override
        String abbreviation() {
            return "m";
        }
    }, HOURS {
        @Override
        String abbreviation() {
            return "h";
        }
    };

    abstract String abbreviation();
}
