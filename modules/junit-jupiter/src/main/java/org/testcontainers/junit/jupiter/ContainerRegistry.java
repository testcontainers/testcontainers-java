package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.lifecycle.Startable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Registry for managing container instances created by {@link ContainerProvider} methods.
 * This class handles both global and class-scoped containers, ensuring proper lifecycle
 * management and thread-safe access.
 */
class ContainerRegistry implements CloseableResource {

    private static final Logger log = LoggerFactory.getLogger(ContainerRegistry.class);

    /**
     * Global registry for containers shared across all test classes.
     * These containers are started once and stopped at JVM shutdown by Ryuk.
     */
    private static final Map<String, ContainerInstance> GLOBAL_CONTAINERS = new ConcurrentHashMap<>();

    /**
     * Class-scoped registry for containers shared within a single test class.
     * These containers are stopped when the test class completes.
     */
    private final Map<String, ContainerInstance> classContainers = new ConcurrentHashMap<>();

    /**
     * Test-scoped registry for containers created with needNewInstance=true.
     * These containers are stopped after the test method completes.
     */
    private final Map<String, ContainerInstance> testContainers = new ConcurrentHashMap<>();

    /**
     * Gets or creates a container instance based on the specified configuration.
     *
     * @param name the container provider name
     * @param scope the container scope
     * @param needNewInstance whether to create a new instance
     * @param factory the factory to create new container instances
     * @return the container instance
     */
    public Startable getOrCreate(
        String name,
        ContainerProvider.Scope scope,
        boolean needNewInstance,
        Supplier<Startable> factory
    ) {
        if (needNewInstance) {
            return createNewInstance(name, factory);
        }

        Map<String, ContainerInstance> registry = getRegistry(scope);
        ContainerInstance instance = registry.computeIfAbsent(name, k -> createAndStartInstance(name, factory));

        return instance.getContainer();
    }

    /**
     * Creates a new container instance that will be managed separately.
     *
     * @param name the container name
     * @param factory the factory to create the container
     * @return the new container instance
     */
    private Startable createNewInstance(String name, Supplier<Startable> factory) {
        log.debug("Creating new instance for container '{}'", name);
        ContainerInstance instance = new ContainerInstance(name, factory.get());
        instance.start();

        // Store in test-scoped registry for cleanup
        testContainers.put(name + "_" + System.nanoTime(), instance);

        return instance.getContainer();
    }

    /**
     * Creates and starts a container instance.
     *
     * @param name the container name
     * @param factory the factory to create the container
     * @return the container instance wrapper
     */
    private ContainerInstance createAndStartInstance(String name, Supplier<Startable> factory) {
        log.debug("Creating and starting container '{}'", name);
        ContainerInstance instance = new ContainerInstance(name, factory.get());
        instance.start();
        return instance;
    }

    /**
     * Gets the appropriate registry based on scope.
     *
     * @param scope the container scope
     * @return the registry map
     */
    private Map<String, ContainerInstance> getRegistry(ContainerProvider.Scope scope) {
        return scope == ContainerProvider.Scope.GLOBAL ? GLOBAL_CONTAINERS : classContainers;
    }

    /**
     * Stops all test-scoped containers (those created with needNewInstance=true).
     */
    public void stopTestContainers() {
        log.debug("Stopping {} test-scoped containers", testContainers.size());
        testContainers.values().forEach(ContainerInstance::stop);
        testContainers.clear();
    }

    /**
     * Stops all class-scoped containers.
     * Called when the test class completes.
     */
    @Override
    public void close() {
        log.debug("Stopping {} class-scoped containers", classContainers.size());
        classContainers.values().forEach(ContainerInstance::stop);
        classContainers.clear();
    }

    /**
     * Gets statistics about the current state of the registry.
     *
     * @return registry statistics
     */
    public RegistryStats getStats() {
        return new RegistryStats(GLOBAL_CONTAINERS.size(), classContainers.size(), testContainers.size());
    }

    /**
     * Clears all global containers. Used primarily for testing.
     */
    static void clearGlobalContainers() {
        log.debug("Clearing {} global containers", GLOBAL_CONTAINERS.size());
        GLOBAL_CONTAINERS.values().forEach(ContainerInstance::stop);
        GLOBAL_CONTAINERS.clear();
    }

    /**
     * Wrapper class for container instances that tracks metadata and lifecycle.
     */
    private static class ContainerInstance {

        private final String name;

        private final Startable container;

        private volatile boolean started = false;

        ContainerInstance(String name, Startable container) {
            this.name = name;
            this.container = container;
        }

        /**
         * Starts the container if not already started.
         * Thread-safe to prevent duplicate starts.
         */
        synchronized void start() {
            if (!started) {
                log.info("Starting container '{}'", name);
                container.start();
                started = true;
                log.info("Container '{}' started successfully", name);
            }
        }

        /**
         * Stops the container if started.
         */
        synchronized void stop() {
            if (started) {
                log.info("Stopping container '{}'", name);
                try {
                    container.stop();
                    started = false;
                    log.info("Container '{}' stopped successfully", name);
                } catch (Exception e) {
                    log.error("Failed to stop container '{}'", name, e);
                }
            }
        }

        Startable getContainer() {
            return container;
        }

        boolean isStarted() {
            return started;
        }
    }

    /**
     * Statistics about the registry state.
     */
    public static class RegistryStats {

        private final int globalContainers;

        private final int classContainers;

        private final int testContainers;

        RegistryStats(int globalContainers, int classContainers, int testContainers) {
            this.globalContainers = globalContainers;
            this.classContainers = classContainers;
            this.testContainers = testContainers;
        }

        public int getGlobalContainers() {
            return globalContainers;
        }

        public int getClassContainers() {
            return classContainers;
        }

        public int getTestContainers() {
            return testContainers;
        }

        public int getTotalContainers() {
            return globalContainers + classContainers + testContainers;
        }

        @Override
        public String toString() {
            return String.format(
                "RegistryStats[global=%d, class=%d, test=%d, total=%d]",
                globalContainers,
                classContainers,
                testContainers,
                getTotalContainers()
            );
        }
    }
}
