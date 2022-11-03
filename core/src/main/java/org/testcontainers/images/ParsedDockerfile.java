package org.testcontainers.images;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Representation of a Dockerfile, with partial parsing for extraction of a minimal set of data.
 */
@Slf4j
public class ParsedDockerfile {

    private static final Pattern FROM_LINE_PATTERN = Pattern.compile(
        "FROM (?<arg>--[^\\s]+\\s)*(?<image>[^\\s]+).*",
        Pattern.CASE_INSENSITIVE
    );

    private final Path dockerFilePath;

    @Getter
    private final Set<String> dependencyImageNames;

    public ParsedDockerfile(Path dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
        this.dependencyImageNames = parse(read());
    }

    @VisibleForTesting
    ParsedDockerfile(List<String> lines) {
        this.dockerFilePath = Paths.get("dummy.Dockerfile");
        this.dependencyImageNames = parse(lines);
    }

    private List<String> read() {
        if (!Files.exists(dockerFilePath)) {
            log.warn("Tried to parse Dockerfile at path {} but none was found", dockerFilePath);
            return Collections.emptyList();
        }

        try {
            return Files.readAllLines(dockerFilePath);
        } catch (IOException e) {
            log.warn("Unable to read Dockerfile at path {}", dockerFilePath, e);
            return Collections.emptyList();
        }
    }

    private Set<String> parse(List<String> lines) {
        Set<String> imageNames = lines
            .stream()
            .map(FROM_LINE_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group("image"))
            .collect(Collectors.toSet());

        if (!imageNames.isEmpty()) {
            log.debug("Found dependency images in Dockerfile {}: {}", dockerFilePath, imageNames);
        }
        return imageNames;
    }
}
