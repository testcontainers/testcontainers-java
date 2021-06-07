package org.testcontainers.containers;

public enum RetentionUnit {
    NANOSECONDS("ns"),
    MICROSECONDS("us"),
    MILLISECONDS("ms"),
    SECONDS("s"),
    MINUTES("m"),
    HOURS("h"),
    DAYS("d"),
    WEEKS("w");

    final String label;

    RetentionUnit(final String label) {
        this.label = label;
    }
}
