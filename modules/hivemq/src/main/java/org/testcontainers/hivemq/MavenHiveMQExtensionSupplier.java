/*
 * MIT License
 *
 * Copyright (c) 2021-present HiveMQ GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
