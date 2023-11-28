package org.testcontainers.images;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

public class ParsedDockerfileTest {

    @Test
    public void doesSimpleParsing() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM someimage", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("extracts a single image name")
            .isEqualTo(Sets.newHashSet("someimage"));
    }

    @Test
    public void isCaseInsensitive() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("from someimage", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("extracts a single image name")
            .isEqualTo(Sets.newHashSet("someimage"));
    }

    @Test
    public void handlesTags() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM someimage:tag", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("retains tags in image names")
            .isEqualTo(Sets.newHashSet("someimage:tag"));
    }

    @Test
    public void handlesDigests() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM someimage@sha256:abc123", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("retains digests in image names")
            .isEqualTo(Sets.newHashSet("someimage@sha256:abc123"));
    }

    @Test
    public void ignoringCommentedFromLines() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM someimage", "#FROM somethingelse")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("ignores commented from lines")
            .isEqualTo(Sets.newHashSet("someimage"));
    }

    @Test
    public void ignoringBuildStageNames() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM someimage --as=base", "RUN something", "FROM nextimage", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("ignores build stage names and allows multiple images to be extracted")
            .isEqualTo(Sets.newHashSet("someimage", "nextimage"));
    }

    @Test
    public void ignoringPlatformArgs() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM --platform=linux/amd64 someimage", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("ignores platform args")
            .isEqualTo(Sets.newHashSet("someimage"));
    }

    @Test
    public void ignoringExtraPlatformArgs() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("FROM --platform=linux/amd64 --somethingelse=value someimage", "RUN something")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("ignores platform args")
            .isEqualTo(Sets.newHashSet("someimage"));
    }

    @Test
    public void handlesGracefullyIfNoFromLine() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(
            Arrays.asList("RUN something", "# is this even a valid Dockerfile?")
        );
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("handles invalid Dockerfiles gracefully")
            .isEqualTo(Sets.newHashSet());
    }

    @Test
    public void handlesGracefullyIfDockerfileNotFound() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(Paths.get("nonexistent.Dockerfile"));
        assertThat(parsedDockerfile.getDependencyImageNames())
            .as("handles missing Dockerfiles gracefully")
            .isEqualTo(Sets.newHashSet());
    }
}
