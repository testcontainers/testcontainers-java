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
import org.jboss.shrinkwrap.resolver.api.maven.embedded.BuiltProject;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.EmbeddedMaven;
import org.jboss.shrinkwrap.resolver.api.maven.embedded.pom.equipped.PomEquippedEmbeddedMaven;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * This class automates the process of packaging a HiveMQ extension from a maven project.
 *
 * @author Yannick Weber
 */
public class MavenHiveMQExtensionSupplier implements Supplier<MountableFile> {

    private final @NotNull String pomFile;
    private boolean quiet = false;
    private final @NotNull Properties properties = new Properties();

    /**
     * This {@link Supplier} can be used if the current maven project is the HiveMQ Extension to supply.
     * It uses the pom.xml file of the current maven project.
     *
     * @return a {@link MavenHiveMQExtensionSupplier} for the current maven project
     */
    public static @NotNull MavenHiveMQExtensionSupplier direct() {
        return new MavenHiveMQExtensionSupplier("pom.xml");
    }

    /**
     * Creates a Maven HiveMQ extension {@link Supplier}.
     *
     * @param pomFile the path of the pom.xml of the HiveMQ extension to supply.
     */
    public MavenHiveMQExtensionSupplier(final @NotNull String pomFile) {
        this.pomFile = pomFile;
    }

    /**
     * Packages the HiveMQ extension, copies it to a temporary directory and returns the directory as a {@link MountableFile}.
     *
     * @return the {@link MountableFile} of the packaged HiveMQ extension
     */
    @Override
    public @NotNull MountableFile get() {
        final PomEquippedEmbeddedMaven embeddedMaven = EmbeddedMaven.forProject(pomFile);
        embeddedMaven
                .setGoals("package")
                .setQuiet(quiet)
                .setBatchMode(true);
        embeddedMaven.setProperties(properties);
        final BuiltProject aPackage = embeddedMaven.build();
        final File targetDirectory = aPackage.getTargetDirectory();
        final String version = aPackage.getModel().getVersion();
        final String artifactId = aPackage.getModel().getArtifactId();

        final ZipFile zipFile = new ZipFile(new File(targetDirectory, artifactId + "-" + version + "-distribution.zip"));

        try {
            final Path tempDirectory = Files.createTempDirectory("");
            zipFile.extractAll(tempDirectory.toString());
            return MountableFile.forHostPath(tempDirectory.resolve(artifactId));
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Suppress stdout of the maven build.
     *
     * @return self
     */
    public @NotNull MavenHiveMQExtensionSupplier quiet() {
        this.quiet = true;
        return this;
    }

    /**
     * Add a custom property for the maven packaging.
     *
     * @param key   the name of the property
     * @param value the value of the property
     * @return self
     */
    public @NotNull MavenHiveMQExtensionSupplier addProperty(final @NotNull String key, final @NotNull String value) {
        properties.put(key, value);
        return this;
    }
}
