package org.testcontainers.containers;

public enum RetentionUnits {
    NANOSECONDS("ns"),
    MICROSECONDS("us"),
    MILLISECONDS("ms"),
    SECONDS("s"),
    MINUTES("m"),
    HOURS("h"),
    DAYS("d"),
    WEEKS("w");

    final String label;

    RetentionUnits(final String label) {
        this.label = label;
    }
}
