/*
 * Copyright 2020 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.testcontainers.hivemq;

import net.lingala.zip4j.ZipFile;
import org.apache.commons.lang3.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class automates the process of packaging a HiveMQ extension from a gradle project.
 * It uses ./gradlew on MacOS and Linux and uses gradlew.bat on Windows to execute the gradle task.
 *
 * @author Yannick Weber
 */
public class GradleHiveMQExtensionSupplier implements Supplier<MountableFile> {

    private static final @NotNull Pattern ROOT_PROJECT_PATTERN = Pattern.compile(".*'(.*)'");
    private static final @NotNull String PROPERTY_REGEX = "(.*): (.*)";

    private static final @NotNull String BUILD_STARTED =
            "=================================================================\n" +
                    "===   Embedded Gradle build started: %s   ===\n" +
                    "=================================================================\n";

    private static final @NotNull String BUILD_STOPPED =
            "=================================================================\n" +
                    "===   Embedded Gradle build stopped: %s   ===\n" +
                    "=================================================================";

    private static final @NotNull String TASK = "hivemqExtensionZip";

    private final @NotNull File gradleProjectDirectory;
    private boolean quiet = false;

    /**
     * Creates a Gradle HiveMQ extension {@link Supplier}.
     * It uses the gradle wrapper of the gradle project associated with the given It uses the build.gradle file.
     *
     * @param gradleProjectDirectory the gradle project directory of the HiveMQ extension to supply.
     */
    public GradleHiveMQExtensionSupplier(final @NotNull File gradleProjectDirectory) {

        if (!gradleProjectDirectory.exists()) {
            throw new IllegalStateException(gradleProjectDirectory + " does not exist.");
        }
        if (!gradleProjectDirectory.canRead()) {
            throw new IllegalStateException(gradleProjectDirectory + " is not readable.");
        }
        this.gradleProjectDirectory = gradleProjectDirectory;
    }

    /**
     * Packages the HiveMQ extension, copies it to a temporary directory and returns the directory as a {@link MountableFile}.
     *
     * @return the {@link MountableFile} of the packaged HiveMQ extension
     */
    @Override
    public @NotNull MountableFile get() {
        System.out.printf((BUILD_STARTED) + "%n", gradleProjectDirectory);

        try {
            final ProcessBuilder extensionZipProcessBuilder = new ProcessBuilder();
            extensionZipProcessBuilder.directory(gradleProjectDirectory);
            extensionZipProcessBuilder.command(getCommandForOs(gradleProjectDirectory), TASK);
            extensionZipProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            if (!quiet) {
                extensionZipProcessBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            }
            final Process extensionZipProcess = extensionZipProcessBuilder.start();
            final int extensionZipExitCode = extensionZipProcess.waitFor();
            if (extensionZipExitCode != 0) {
                throw new IllegalStateException("Gradle build exited with code " + extensionZipExitCode);
            }

            final ProcessBuilder propertiesProcessBuilder = new ProcessBuilder();
            propertiesProcessBuilder.directory(gradleProjectDirectory);
            propertiesProcessBuilder.command(getCommandForOs(gradleProjectDirectory), "properties", "-q");
            propertiesProcessBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);

            final Process propertiesProcess = propertiesProcessBuilder.start();
            final int propertiesExitCode = propertiesProcess.waitFor();
            if (propertiesExitCode != 0) {
                throw new IllegalStateException("Gradle build exited with code " + propertiesExitCode);
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(propertiesProcess.getInputStream()));

            final Map<String, String> gradleProperties = br.lines()
                    .filter(s -> s.matches(PROPERTY_REGEX))
                    .map(s -> {
                        final String[] splits = s.split(": ");
                        return new AbstractMap.SimpleEntry<>(splits[0], splits[1]);
                    })
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            System.out.printf((BUILD_STOPPED) + "%n", this.gradleProjectDirectory);

            final String projectVersion = gradleProperties.get("version");
            final String rootProject = gradleProperties.get("rootProject");
            final Matcher matcher = ROOT_PROJECT_PATTERN.matcher(rootProject);
            final boolean b = matcher.find();
            final String projectName = matcher.group(1);

            final ZipFile zipFile = new ZipFile(
                    new File(gradleProjectDirectory,
                            "build/hivemq-extension/" + projectName + "-" + projectVersion + ".zip"));
            final Path tempDirectory = Files.createTempDirectory("");

            zipFile.extractAll(tempDirectory.toString());
            return MountableFile.forHostPath(tempDirectory.resolve(projectName));

        } catch (final Exception e) {
            throw new RuntimeException("Exception while building the HiveMQ extension with gradle.", e);
        }
    }

    private @NotNull String getCommandForOs(final @NotNull File gradleProjectFile) {
        if (SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_MAC_OSX || SystemUtils.IS_OS_LINUX) {
            final String gradleWrapper = gradleProjectFile + "/gradlew";
            final File gradleWrapperBashFile = new File(gradleWrapper);
            if (gradleWrapperBashFile.exists()) {
                if (!gradleWrapperBashFile.canExecute()) {
                    throw new IllegalStateException("Gradle Wrapper " + gradleWrapperBashFile.getAbsolutePath() + " can not be executed.");
                }
                return gradleWrapperBashFile.getAbsolutePath();
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            final String gradleWrapperBat = gradleProjectFile + "/gradlew.bat";
            final File gradleWrapperBatFile = new File(gradleWrapperBat);
            if (gradleWrapperBatFile.exists()) {
                if (!gradleWrapperBatFile.canExecute()) {
                    throw new IllegalStateException("Gradle Wrapper " + gradleWrapperBatFile.getAbsolutePath() + " can not be executed.");
                }
                return gradleWrapperBatFile.getAbsolutePath();
            }
        }
        throw new IllegalStateException("Unknown OS Version");

    }

    /**
     * Suppress stdout of the gradle build.
     *
     * @return self
     */
    @NotNull
    public GradleHiveMQExtensionSupplier quiet() {
        this.quiet = true;
        return this;
    }
}
