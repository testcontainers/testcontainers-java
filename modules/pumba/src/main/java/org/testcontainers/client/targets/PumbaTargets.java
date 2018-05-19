package org.testcontainers.client.targets;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by novy on 01.01.17.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PumbaTargets {

    public static PumbaTarget containers(String... containerNames) {
        return containers(Arrays.asList(containerNames));
    }

    public static PumbaTarget containers(Collection<String> containerNames) {
        return () -> containerNames.stream().collect(Collectors.joining(" "));
    }

    public static PumbaTarget containersMatchingRegexp(String regex) {
        return () -> "re2:" + regex;
    }

    public static PumbaTarget containersMatchingRegexp(Pattern regex) {
        return containersMatchingRegexp(regex.pattern());
    }
}
