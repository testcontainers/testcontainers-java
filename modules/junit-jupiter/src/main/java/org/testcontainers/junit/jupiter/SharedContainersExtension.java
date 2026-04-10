package org.testcontainers.junit.jupiter;

import lombok.Getter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JUnit Jupiter Extension that backs the {@link SharedContainers} annotation.
 *
 * <p>Unlike {@link TestcontainersExtension}, which stores containers in a class-scoped
 * {@link ExtensionContext.Store} (causing restart per test class), this extension stores
 * {@code static} {@link Container}-annotated fields in the <em>root</em> store so that
 * they are started at most once per JVM session and live until the JVM exits.</p>
 */
public class SharedContainersExtension implements BeforeEachCallback, BeforeAllCallback, AfterEachCallback, ExecutionCondition {

    private static final Namespace NAMESPACE = Namespace.create(SharedContainersExtension.class);

    private static final String LOCAL_LIFECYCLE_AWARE_CONTAINERS = "localLifecycleAwareContainers";

    private final DockerAvailableDetector dockerDetector = new DockerAvailableDetector();

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> testClass = context
            .getTestClass()
            .orElseThrow(() -> new ExtensionConfigurationException("SharedContainersExtension is only supported for classes."));

        // Use ROOT store so containers survive across test class boundaries.
        Store rootStore = context.getRoot().getStore(NAMESPACE);
        List<StoreAdapter> sharedContainersStoreAdapters = findSharedContainers(testClass);

        startContainersInStore(sharedContainersStoreAdapters, rootStore, context);

        List<TestLifecycleAware> lifecycleAwareContainers = sharedContainersStoreAdapters
            .stream()
            .filter(this::isTestLifecycleAware)
            .map(adapter -> (TestLifecycleAware) adapter.container)
            .collect(Collectors.toList());

        signalBeforeTestToContainers(lifecycleAwareContainers, testDescriptionFrom(context));
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);

        List<StoreAdapter> restartContainers = collectParentTestInstances(context)
            .parallelStream()
            .flatMap(this::findRestartContainers)
            .collect(Collectors.toList());

        List<TestLifecycleAware> lifecycleAwareContainers = startContainersAndCollectLifecycleAware(
            restartContainers,
            store,
            context
        );

        store.put(LOCAL_LIFECYCLE_AWARE_CONTAINERS, lifecycleAwareContainers);
        signalBeforeTestToContainers(lifecycleAwareContainers, testDescriptionFrom(context));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        List<TestLifecycleAware> containers = (List<TestLifecycleAware>) context
            .getStore(NAMESPACE)
            .get(LOCAL_LIFECYCLE_AWARE_CONTAINERS);
        if (containers != null) {
            TestDescription description = testDescriptionFrom(context);
            Optional<Throwable> throwable = context.getExecutionException();
            containers.forEach(c -> c.afterTest(description, throwable));
        }
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findSharedContainersAnnotation(context)
            .map(this::evaluate)
            .orElseThrow(() -> new ExtensionConfigurationException("@SharedContainers not found"));
    }

    private ConditionEvaluationResult evaluate(SharedContainers annotation) {
        if (annotation.disabledWithoutDocker()) {
            if (dockerDetector.isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
            return ConditionEvaluationResult.disabled("disabledWithoutDocker is true and Docker is not available");
        }
        return ConditionEvaluationResult.enabled("disabledWithoutDocker is false");
    }

    private Optional<SharedContainers> findSharedContainersAnnotation(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Optional<SharedContainers> annotation = AnnotationSupport.findAnnotation(
                current.get().getRequiredTestClass(),
                SharedContainers.class
            );
            if (annotation.isPresent()) {
                return annotation;
            }
            current = current.get().getParent();
        }
        return Optional.empty();
    }

    private boolean isParallelExecutionEnabled(ExtensionContext context) {
        return findSharedContainersAnnotation(context).map(SharedContainers::parallel).orElse(false);
    }

    private void startContainersInStore(List<StoreAdapter> adapters, Store store, ExtensionContext context) {
        if (adapters.isEmpty()) {
            return;
        }
        if (isParallelExecutionEnabled(context)) {
            Stream<Startable> startables = adapters.stream().map(adapter -> {
                store.getOrComputeIfAbsent(adapter.getKey(), k -> adapter);
                return adapter.container;
            });
            Startables.deepStart(startables).join();
        } else {
            adapters.forEach(adapter -> store.getOrComputeIfAbsent(adapter.getKey(), k -> adapter.start()));
        }
    }

    private List<TestLifecycleAware> startContainersAndCollectLifecycleAware(
        List<StoreAdapter> adapters,
        Store store,
        ExtensionContext context
    ) {
        startContainersInStore(adapters, store, context);
        return adapters
            .stream()
            .filter(this::isTestLifecycleAware)
            .map(adapter -> (TestLifecycleAware) adapter.container)
            .collect(Collectors.toList());
    }

    private List<StoreAdapter> findSharedContainers(Class<?> testClass) {
        return ReflectionSupport
            .findFields(testClass, isSharedContainer(), HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .map(f -> getContainerInstance(null, f))
            .collect(Collectors.toList());
    }

    private Predicate<Field> isSharedContainer() {
        return isContainer().and(ModifierSupport::isStatic);
    }

    private Stream<StoreAdapter> findRestartContainers(Object testInstance) {
        return ReflectionSupport
            .findFields(testInstance.getClass(), isRestartContainer(), HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .map(f -> getContainerInstance(testInstance, f));
    }

    private Predicate<Field> isRestartContainer() {
        return isContainer().and(ModifierSupport::isNotStatic);
    }

    private static Predicate<Field> isContainer() {
        return field -> {
            boolean isAnnotatedWithContainer = AnnotationSupport.isAnnotated(field, Container.class);
            if (isAnnotatedWithContainer) {
                boolean isStartable = Startable.class.isAssignableFrom(field.getType());
                if (!isStartable) {
                    throw new ExtensionConfigurationException(
                        String.format("FieldName: %s does not implement Startable", field.getName())
                    );
                }
                return true;
            }
            return false;
        };
    }

    private static StoreAdapter getContainerInstance(Object testInstance, Field field) {
        try {
            field.setAccessible(true);
            Startable containerInstance = (Startable) field.get(testInstance);
            if (containerInstance == null) {
                throw new ExtensionConfigurationException("Container " + field.getName() + " needs to be initialized");
            }
            return new StoreAdapter(field.getDeclaringClass(), field.getName(), containerInstance);
        } catch (IllegalAccessException e) {
            throw new ExtensionConfigurationException("Can not access container defined in field " + field.getName());
        }
    }

    private boolean isTestLifecycleAware(StoreAdapter adapter) {
        return adapter.container instanceof TestLifecycleAware;
    }

    private void signalBeforeTestToContainers(List<TestLifecycleAware> containers, TestDescription description) {
        containers.forEach(c -> c.beforeTest(description));
    }

    private Set<Object> collectParentTestInstances(ExtensionContext context) {
        List<Object> allInstances = new ArrayList<>(context.getRequiredTestInstances().getAllInstances());
        Collections.reverse(allInstances);
        return new LinkedHashSet<>(allInstances);
    }

    private TestDescription testDescriptionFrom(ExtensionContext context) {
        return new TestcontainersTestDescription(
            context.getUniqueId(),
            FilesystemFriendlyNameGenerator.filesystemFriendlyNameOf(context)
        );
    }

    /**
     * An adapter for {@link Startable} that implements {@link CloseableResource},
     * letting JUnit automatically stop containers once the {@link ExtensionContext} is closed.
     *
     * <p>When stored in the root store, {@code close()} is called at the very end of the
     * test suite (JVM shutdown), ensuring shared containers live for the full session.</p>
     */
    private static class StoreAdapter implements CloseableResource, AutoCloseable {

        @Getter
        private final String key;

        private final Startable container;

        private StoreAdapter(Class<?> declaringClass, String fieldName, Startable container) {
            this.key = declaringClass.getName() + "." + fieldName;
            this.container = container;
        }

        private StoreAdapter start() {
            container.start();
            return this;
        }

        @Override
        public void close() {
            container.stop();
        }
    }
}
