package org.testcontainers.utility;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class PathUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void findCommonParentOfSingleFileIsItsOwnDirectory() throws IOException {
        File composeFile = createFile(tempDir, "docker-compose.yml");

        assertThat(PathUtils.findCommonParent(Collections.singletonList(composeFile))).isEqualTo(tempDir.toFile());
    }

    @Test
    void findCommonParentOfFilesInTheSameDirectory() throws IOException {
        File baseFile = createFile(tempDir, "docker-compose.yml");
        File overrideFile = createFile(tempDir, "docker-compose.override.yml");

        assertThat(PathUtils.findCommonParent(Arrays.asList(baseFile, overrideFile))).isEqualTo(tempDir.toFile());
    }

    @Test
    void findCommonParentOfFilesInDifferentDirectories() throws IOException {
        // Mirrors the scenario from testcontainers/testcontainers-java#1863: a base compose file
        // nested in a subdirectory, and an override file one level up.
        Path firstDir = tempDir.resolve("first");
        Path nestedDir = firstDir.resolve("directory");
        Files.createDirectories(nestedDir);

        File baseFile = createFile(nestedDir, "docker-compose.yml");
        File overrideFile = createFile(firstDir, "docker-compose.override.yml");

        assertThat(PathUtils.findCommonParent(Arrays.asList(baseFile, overrideFile))).isEqualTo(firstDir.toFile());
    }

    @Test
    void findCommonParentIsOrderIndependent() throws IOException {
        Path firstDir = tempDir.resolve("first");
        Path nestedDir = firstDir.resolve("directory");
        Files.createDirectories(nestedDir);

        File baseFile = createFile(nestedDir, "docker-compose.yml");
        File overrideFile = createFile(firstDir, "docker-compose.override.yml");

        assertThat(PathUtils.findCommonParent(Arrays.asList(overrideFile, baseFile))).isEqualTo(firstDir.toFile());
    }

    @Test
    void findCommonParentOfFilesInSiblingDirectories() throws IOException {
        Path branchA = tempDir.resolve("a");
        Path branchB = tempDir.resolve("b");
        Files.createDirectories(branchA);
        Files.createDirectories(branchB);

        File fileA = createFile(branchA, "docker-compose.yml");
        File fileB = createFile(branchB, "docker-compose.override.yml");

        assertThat(PathUtils.findCommonParent(Arrays.asList(fileA, fileB))).isEqualTo(tempDir.toFile());
    }

    private static File createFile(Path directory, String name) throws IOException {
        Path file = directory.resolve(name);
        Files.write(file, new byte[0]);
        return file.toFile();
    }
}
