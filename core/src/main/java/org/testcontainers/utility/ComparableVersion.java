package org.testcontainers.utility;

import java.util.ArrayList;
import java.util.List;

import com.google.common.annotations.VisibleForTesting;

import org.jetbrains.annotations.NotNull;

public final class ComparableVersion implements Comparable<ComparableVersion> {

    private final int[] parts;

    public static final ComparableVersion OS_VERSION = new ComparableVersion(System.getProperty("os.version"));

    public ComparableVersion(String version) {
        this.parts = parseVersion(version);
    }

    @Override
    public int compareTo(@NotNull ComparableVersion other) {
        for (int i=0; i<Math.min(this.parts.length, other.parts.length); i++) {
            int thisPart = this.parts[i];
            int otherPart = other.parts[i];
            if (thisPart > otherPart) {
                return 1;
            } else if (thisPart < otherPart) {
                return -1;
            }
        }

        return 0;
    }

    public boolean isSemanticVersion() {
        return parts.length > 0;
    }

    public boolean isLessThan(String other) {
        return this.compareTo(new ComparableVersion(other)) < 0;
    }

    public boolean isGreaterThanOrEqualTo(String other) {
        return this.compareTo(new ComparableVersion(other)) >= 0;
    }

    @VisibleForTesting
    static int[] parseVersion(final String version) {
        final List<Integer> parts = new ArrayList<>(5);

        int acc = 0;
        for (final char c : version.toCharArray()) {
            if (c == '.') {
                parts.add(acc);
                acc = 0;
            }

            if (Character.isDigit(c)) {
                acc = 10 * acc + Character.digit(c, 10);
            }
        }

        if (acc != 0) {
            parts.add(acc);
        }

        final int[] ret = new int[parts.size()];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = parts.get(i);
        }

        return ret;
    }

}
