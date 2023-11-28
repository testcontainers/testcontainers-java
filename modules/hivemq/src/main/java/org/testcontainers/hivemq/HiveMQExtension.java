package org.testcontainers.hivemq;

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.jboss.shrinkwrap.api.ExtensionLoader;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.impl.base.spec.JavaArchiveImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.ContainerLaunchException;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javassist.ClassPool;
import javassist.NotFoundException;

public class HiveMQExtension {

    private static final String VALID_EXTENSION_XML =
        "<hivemq-extension>" + //
        "   <id>%s</id>" + //
        "   <name>%s</name>" + //
        "   <version>%s</version>" + //
        "   <priority>%s</priority>" + //
        "   <start-priority>%s</start-priority>" + //
        "</hivemq-extension>";

    private static final String EXTENSION_MAIN_CLASS_NAME = "com.hivemq.extension.sdk.api.ExtensionMain";

    private static final Logger LOGGER = LoggerFactory.getLogger(HiveMQExtension.class);

    @Getter
    @NotNull
    private final String id;

    @Getter
    @NotNull
    private final String name;

    @Getter
    @NotNull
    private final String version;

    @Getter
    private final int priority;

    @Getter
    private final int startPriority;

    @Getter
    private final boolean disabledOnStartup;

    @Getter
    @NotNull
    private final Class<?> mainClass;

    @NotNull
    private final List<Class<?>> additionalClasses;

    private HiveMQExtension(
        final @NotNull String id,
        final @NotNull String name,
        final @NotNull String version,
        final int priority,
        final int startPriority,
        final boolean disabledOnStartup,
        final @NotNull Class<?> mainClass,
        final @NotNull List<Class<?>> additionalClasses
    ) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.priority = priority;
        this.startPriority = startPriority;
        this.disabledOnStartup = disabledOnStartup;
        this.mainClass = mainClass;
        this.additionalClasses = additionalClasses;
    }

    @NotNull
    File createExtension(final @NotNull HiveMQExtension hiveMQExtension) throws Exception {
        final File tempDir = Files.createTempDirectory("").toFile();

        final File extensionDir = new File(tempDir, hiveMQExtension.getId());
        FileUtils.writeStringToFile(
            new File(extensionDir, "hivemq-extension.xml"),
            String.format(
                VALID_EXTENSION_XML,
                hiveMQExtension.getId(),
                hiveMQExtension.getName(),
                hiveMQExtension.getVersion(),
                hiveMQExtension.getPriority(),
                hiveMQExtension.getStartPriority()
            ),
            Charset.defaultCharset()
        );

        if (hiveMQExtension.isDisabledOnStartup()) {
            final File disabled = new File(extensionDir, "DISABLED");
            final boolean newFile = disabled.createNewFile();
            if (!newFile) {
                throw new ContainerLaunchException(
                    "Could not create DISABLED file '" + disabled.getAbsolutePath() + "' on host machine."
                );
            }
        }

        // Shadow Gradle plugin doesn't know how to handle ShrinkWrap's SPI definitions
        // This workaround creates the mappings programmatically
        // TODO write a custom Gradle Shadow transformer?
        ExtensionLoader extensionLoader = ShrinkWrap.getDefaultDomain().getConfiguration().getExtensionLoader();
        extensionLoader.addOverride(JavaArchive.class, JavaArchiveImpl.class);
        extensionLoader.addOverride(ZipExporter.class, ZipExporterImpl.class);

        final JavaArchive javaArchive = ShrinkWrap
            .create(JavaArchive.class)
            .addAsServiceProvider(EXTENSION_MAIN_CLASS_NAME, hiveMQExtension.getMainClass().getName());

        putSubclassesIntoJar(hiveMQExtension.getId(), hiveMQExtension.getMainClass(), javaArchive);
        for (final Class<?> additionalClass : hiveMQExtension.getAdditionalClasses()) {
            javaArchive.addClass(additionalClass);
            putSubclassesIntoJar(hiveMQExtension.getId(), additionalClass, javaArchive);
        }

        javaArchive.as(ZipExporter.class).exportTo(new File(extensionDir, "extension.jar"));

        return extensionDir;
    }

    private void putSubclassesIntoJar(
        final @NotNull String extensionId,
        final @Nullable Class<?> clazz,
        final @NotNull JavaArchive javaArchive
    ) throws NotFoundException {
        if (clazz != null) {
            final Set<String> subClassNames = ClassPool
                .getDefault()
                .get(clazz.getName())
                .getClassFile()
                .getConstPool()
                .getClassNames();
            for (final String subClassName : subClassNames) {
                final String className = subClassName.replaceAll("/", ".");

                if (!className.startsWith("[L")) {
                    LOGGER.debug("Trying to package subclass '{}' into extension '{}'.", className, extensionId);
                    javaArchive.addClass(className);
                } else {
                    LOGGER.debug("Class '{}' will be ignored.", className);
                }
            }
        }
    }

    public @NotNull List<Class<?>> getAdditionalClasses() {
        return Collections.unmodifiableList(additionalClasses);
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        @Nullable
        private String id;

        @Nullable
        private String name;

        @Nullable
        private String version;

        private int priority = 0;

        private int startPriority = 0;

        private boolean disabledOnStartup = false;

        @Nullable
        private Class<?> mainClass;

        @NotNull
        private final LinkedList<Class<?>> additionalClasses = new LinkedList<>();

        /**
         * Builds the {@link HiveMQExtension} with the provided values or default values.
         * @return the HiveMQ Extension
         */
        public @NotNull HiveMQExtension build() {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("extension id must not be null or empty");
            }
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("extension name must not be null or empty");
            }
            if (version == null || version.isEmpty()) {
                throw new IllegalArgumentException("extension version must not be null or empty");
            }
            if (mainClass == null) {
                throw new IllegalArgumentException("extension main class must not be null");
            }
            return new HiveMQExtension(
                id,
                name,
                version,
                priority,
                startPriority,
                disabledOnStartup,
                mainClass,
                additionalClasses
            );
        }

        /**
         * Sets the identifier of the {@link HiveMQExtension}.
         *
         * @param id the identifier, must not be empty
         * @return the {@link Builder}
         */
        public @NotNull Builder id(final @NotNull String id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the name of the {@link HiveMQExtension}.
         *
         * @param name the identifier, must not be empty
         * @return the {@link Builder}
         */
        public @NotNull Builder name(final @NotNull String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets the version of the {@link HiveMQExtension}.
         *
         * @param version the version, must not be empty
         * @return the {@link Builder}
         */
        public @NotNull Builder version(final @NotNull String version) {
            this.version = version;
            return this;
        }

        /**
         * Sets the priority of the {@link HiveMQExtension}.
         *
         * @param priority the priority
         * @return the {@link Builder}
         */
        public @NotNull Builder priority(final int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * Sets the start-priority of the {@link HiveMQExtension}.
         *
         * @param startPriority the start-priority
         * @return the {@link Builder}
         */
        public @NotNull Builder startPriority(final int startPriority) {
            this.startPriority = startPriority;
            return this;
        }

        /**
         * Flag, that indicates whether the {@link HiveMQExtension} should be disabled when HiveMQ starts.
         * Disabling on startup is achieved by placing a DISABLED file in the {@link HiveMQExtension}'s directory before coping it to the container.
         *
         * @param disabledOnStartup if the {@link HiveMQExtension} should be disabled when HiveMQ starts
         * @return the {@link Builder}
         */
        public @NotNull Builder disabledOnStartup(final boolean disabledOnStartup) {
            this.disabledOnStartup = disabledOnStartup;
            return this;
        }

        /**
         * The main class of the {@link HiveMQExtension}.
         * This class MUST implement com.hivemq.extension.sdk.api.ExtensionMain.
         *
         * @param mainClass the main class
         * @return the {@link Builder}
         * @throws IllegalArgumentException if the provides class does not implement com.hivemq.extension.sdk.api.ExtensionMain}
         * @throws IllegalStateException if com.hivemq.extension.sdk.api.ExtensionMain is not found in the classpath
         */
        public @NotNull Builder mainClass(final @NotNull Class<?> mainClass) {
            try {
                final Class<?> extensionMain = Class.forName(EXTENSION_MAIN_CLASS_NAME);
                if (!extensionMain.isAssignableFrom(mainClass)) {
                    throw new IllegalArgumentException(
                        "The provided class does not implement '" + EXTENSION_MAIN_CLASS_NAME + "'"
                    );
                }
                this.mainClass = mainClass;
                return this;
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException(
                    "The class '" + EXTENSION_MAIN_CLASS_NAME + "' was not found in the classpath."
                );
            }
        }

        /**
         * Adds an additional class to the .jar file of the {@link HiveMQExtension}.
         *
         * @param clazz the additional class
         * @return the {@link Builder}
         */
        public @NotNull Builder addAdditionalClass(final @NotNull Class<?> clazz) {
            this.additionalClasses.add(clazz);
            return this;
        }
    }
}
