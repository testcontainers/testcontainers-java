package org.testcontainers.hivemq;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Yannick Weber
 */
public class HiveMQExtension {

    private final @NotNull String id;
    private final @NotNull String name;
    private final @NotNull String version;
    private final int priority;
    private final int startPriority;
    private final boolean disabledOnStartup;
    private final @NotNull Class<?> mainClass;
    private final @NotNull List<Class<?>> additionalClasses;

    private HiveMQExtension(
        final @NotNull String id,
        final @NotNull String name,
        final @NotNull String version,
        final int priority,
        final int startPriority,
        final boolean disabledOnStartup,
        final @NotNull Class<?> mainClass,
        final @NotNull List<Class<?>> additionalClasses) {

        this.id = id;
        this.name = name;
        this.version = version;
        this.priority = priority;
        this.startPriority = startPriority;
        this.disabledOnStartup = disabledOnStartup;
        this.mainClass = mainClass;
        this.additionalClasses = additionalClasses;
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getVersion() {
        return version;
    }

    public int getPriority() {
        return priority;
    }

    public int getStartPriority() {
        return startPriority;
    }

    public boolean isDisabledOnStartup() {
        return disabledOnStartup;
    }

    public @NotNull Class<?> getMainClass() {
        return mainClass;
    }

    public @NotNull List<Class<?>> getAdditionalClasses() {
        return Collections.unmodifiableList(additionalClasses);
    }

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String version;
        private int priority = 0;
        private int startPriority = 0;
        private boolean disabledOnStartup = false;
        private @Nullable Class<?> mainClass;
        private final @NotNull LinkedList<Class<?>> additionalClasses = new LinkedList<>();

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
                final Class<?> extensionMain = Class.forName(HiveMQContainer.EXTENSION_MAIN_CLASS_NAME);
                if (!extensionMain.isAssignableFrom(mainClass)) {
                    throw new IllegalArgumentException("The provided class does not implement '" + HiveMQContainer.EXTENSION_MAIN_CLASS_NAME + "'");
                }
                this.mainClass = mainClass;
                return this;
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException("The class '" + HiveMQContainer.EXTENSION_MAIN_CLASS_NAME + "' was not found in the classpath.");
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
