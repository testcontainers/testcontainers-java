package org.testcontainers.images;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Representation of a Dockerfile, with partial parsing for extraction of a minimal set of data.
 */
@Slf4j
public class ParsedDockerfile {

    private static final Pattern FROM_LINE_PATTERN = Pattern.compile(
        "FROM (?<arg>--[^\\s]+\\s)*(?<image>[^\\s]+)(\\s+AS\\s+(?<stage>[^\\s]+))?.*",
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
        Set<String> imageNames = new HashSet<>();
        Set<String> buildStageNames = new HashSet<>();

        for (String line : lines) {
            Matcher matcher = FROM_LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            // A FROM referring to a previously declared build stage (e.g. `FROM builder`) is an internal
            // stage reference, not an external image to be pulled. Build stage names are case-insensitive.
            String image = matcher.group("image");
            if (!buildStageNames.contains(image.toLowerCase(Locale.ROOT))) {
                imageNames.add(image);
            }
            String stage = matcher.group("stage");
            if (stage != null) {
                buildStageNames.add(stage.toLowerCase(Locale.ROOT));
            }
        }

        if (!imageNames.isEmpty()) {
            log.debug("Found dependency images in Dockerfile {}: {}", dockerFilePath, imageNames);
        }
        return imageNames;
    }
}
