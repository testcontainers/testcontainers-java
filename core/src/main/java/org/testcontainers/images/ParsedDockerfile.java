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

    private static final Pattern FROM_LINE_PATTERN = Pattern.compile("FROM (?<arg>--[^\\s]+\\s)*(?<image>[^\\s]+).*", Pattern.CASE_INSENSITIVE);

    private final Path dockerFilePath;

    @Getter
    private Set<String> dependencyImageNames = Collections.emptySet();

    public ParsedDockerfile(Path dockerFilePath) {
        this.dockerFilePath = dockerFilePath;
        parse(read());
    }

    @VisibleForTesting
    ParsedDockerfile(List<String> lines) {
        this.dockerFilePath = Paths.get("dummy.Dockerfile");
        parse(lines);
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

    private void parse(List<String> lines) {
        dependencyImageNames = lines.stream()
            .map(FROM_LINE_PATTERN::matcher)
            .filter(Matcher::matches)
            .map(matcher -> matcher.group("image"))
            .collect(Collectors.toSet());

        if (!dependencyImageNames.isEmpty()) {
            log.debug("Found dependency images in Dockerfile {}: {}", dockerFilePath, dependencyImageNames);
        }
    }
}
