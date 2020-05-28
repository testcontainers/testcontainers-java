package org.testcontainers.images;

import com.google.common.collect.Sets;
import org.junit.Test;

import java.nio.file.Paths;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class ParsedDockerfileTest {

    @Test
    public void doesSimpleParsing() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM someimage", "RUN something"));
        assertEquals("extracts a single image name", Sets.newHashSet("someimage"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void isCaseInsensitive() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("from someimage", "RUN something"));
        assertEquals("extracts a single image name", Sets.newHashSet("someimage"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void handlesTags() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM someimage:tag", "RUN something"));
        assertEquals("retains tags in image names", Sets.newHashSet("someimage:tag"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void handlesDigests() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM someimage@sha256:abc123", "RUN something"));
        assertEquals("retains digests in image names", Sets.newHashSet("someimage@sha256:abc123"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void ignoringCommentedFromLines() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM someimage", "#FROM somethingelse"));
        assertEquals("ignores commented from lines", Sets.newHashSet("someimage"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void ignoringBuildStageNames() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM someimage --as=base", "RUN something", "FROM nextimage", "RUN something"));
        assertEquals("ignores build stage names and allows multiple images to be extracted", Sets.newHashSet("someimage", "nextimage"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void ignoringPlatformArgs() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM --platform=linux/amd64 someimage", "RUN something"));
               assertEquals("ignores platform args", Sets.newHashSet("someimage"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void ignoringExtraPlatformArgs() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("FROM --platform=linux/amd64 --somethingelse=value someimage", "RUN something"));
               assertEquals("ignores platform args", Sets.newHashSet("someimage"), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void handlesGracefullyIfNoFromLine() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(asList("RUN something", "# is this even a valid Dockerfile?"));
        assertEquals("handles invalid Dockerfiles gracefully", Sets.newHashSet(), parsedDockerfile.getDependencyImageNames());
    }

    @Test
    public void handlesGracefullyIfDockerfileNotFound() {
        final ParsedDockerfile parsedDockerfile = new ParsedDockerfile(Paths.get("nonexistent.Dockerfile"));
        assertEquals("handles missing Dockerfiles gracefully", Sets.newHashSet(), parsedDockerfile.getDependencyImageNames());
    }
}
