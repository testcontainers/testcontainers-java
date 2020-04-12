package org.testcontainers.junit.jupiter;

import lombok.Getter;
import org.junit.jupiter.api.extension.AfterAllCallback;
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
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.AnnotationUtils;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.testcontainers.junit.jupiter.FilesystemFriendlyNameGenerator.filesystemFriendlyNameOf;

class TestcontainersExtension implements BeforeEachCallback, BeforeAllCallback, AfterEachCallback, AfterAllCallback, ExecutionCondition, TestInstancePostProcessor {

    private static final Namespace NAMESPACE = Namespace.create(TestcontainersExtension.class);

    private static final String TEST_INSTANCE = "testInstance";
    private static final String SHARED_LIFECYCLE_AWARE_CONTAINERS = "sharedLifecycleAwareContainers";
    private static final String LOCAL_LIFECYCLE_AWARE_CONTAINERS = "localLifecycleAwareContainers";

    @Override
    public void postProcessTestInstance(final Object testInstance, final ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);
        store.put(TEST_INSTANCE, testInstance);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        Class<?> testClass = context.getTestClass()
            .orElseThrow(() -> new ExtensionConfigurationException("TestcontainersExtension is only supported for classes."));

        Store store = context.getStore(NAMESPACE);
        List<StoreAdapter> sharedContainersStoreAdapters = findSharedContainers(testClass);

        sharedContainersStoreAdapters.forEach(adapter -> store.getOrComputeIfAbsent(adapter.getKey(), k -> adapter.start()));

        List<TestLifecycleAware> lifecycleAwareContainers = sharedContainersStoreAdapters
            .stream()
            .filter(this::isTestLifecycleAware)
            .map(lifecycleAwareAdapter -> (TestLifecycleAware) lifecycleAwareAdapter.container)
            .collect(toList());

        store.put(SHARED_LIFECYCLE_AWARE_CONTAINERS, lifecycleAwareContainers);
        signalBeforeTestToContainers(lifecycleAwareContainers, testDescriptionFrom(context));
    }

    @Override
    public void afterAll(ExtensionContext context) {
        signalAfterTestToContainersFor(SHARED_LIFECYCLE_AWARE_CONTAINERS, context);
    }

    @Override
    public void beforeEach(final ExtensionContext context) {
        Store store = context.getStore(NAMESPACE);

        List<TestLifecycleAware> lifecycleAwareContainers = collectParentTestInstances(context).parallelStream()
            .flatMap(this::findRestartContainers)
            .peek(adapter -> store.getOrComputeIfAbsent(adapter.getKey(), k -> adapter.start()))
            .filter(this::isTestLifecycleAware)
            .map(lifecycleAwareAdapter -> (TestLifecycleAware) lifecycleAwareAdapter.container)
            .collect(toList());

        store.put(LOCAL_LIFECYCLE_AWARE_CONTAINERS, lifecycleAwareContainers);
        signalBeforeTestToContainers(lifecycleAwareContainers, testDescriptionFrom(context));
    }

    @Override
    public void afterEach(ExtensionContext context) {
        signalAfterTestToContainersFor(LOCAL_LIFECYCLE_AWARE_CONTAINERS, context);
    }

    private void signalBeforeTestToContainers(List<TestLifecycleAware> lifecycleAwareContainers, TestDescription testDescription) {
        lifecycleAwareContainers.forEach(container -> container.beforeTest(testDescription));
    }

    private void signalAfterTestToContainersFor(String storeKey, ExtensionContext context) {
        List<TestLifecycleAware> lifecycleAwareContainers =
            (List<TestLifecycleAware>) context.getStore(NAMESPACE).get(storeKey);
        if (lifecycleAwareContainers != null) {
            TestDescription description = testDescriptionFrom(context);
            Optional<Throwable> throwable = context.getExecutionException();
            lifecycleAwareContainers.forEach(container -> container.afterTest(description, throwable));
        }
    }

    private TestDescription testDescriptionFrom(ExtensionContext context) {
        return new TestcontainersTestDescription(
            context.getUniqueId(),
            filesystemFriendlyNameOf(context)
        );
    }

    private boolean isTestLifecycleAware(StoreAdapter adapter) {
        return adapter.container instanceof TestLifecycleAware;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        return findTestcontainers(context).map(this::evaluate)
            .orElseThrow(() -> new ExtensionConfigurationException("@Testcontainers not found"));
    }

    private Optional<Testcontainers> findTestcontainers(ExtensionContext context) {
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            Optional<Testcontainers> testcontainers = AnnotationUtils.findAnnotation(current.get().getRequiredTestClass(), Testcontainers.class);
            if (testcontainers.isPresent()) {
                return testcontainers;
            }
            current = current.get().getParent();
        }
        return Optional.empty();
    }

    private ConditionEvaluationResult evaluate(Testcontainers testcontainers) {
        if (testcontainers.disabledWithoutDocker()) {
            if (isDockerAvailable()) {
                return ConditionEvaluationResult.enabled("Docker is available");
            }
            return ConditionEvaluationResult.disabled("disabledWithoutDocker is true and Docker is not available");
        }
        return ConditionEvaluationResult.enabled("disabledWithoutDocker is false");
    }

    boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private Set<Object> collectParentTestInstances(final ExtensionContext context) {
        Set<Object> testInstances = new LinkedHashSet<>();
        Optional<ExtensionContext> current = Optional.of(context);
        while (current.isPresent()) {
            ExtensionContext ctx = current.get();
            Object testInstance = ctx.getStore(NAMESPACE).remove(TEST_INSTANCE);
            if (testInstance != null) {
                testInstances.add(testInstance);
            }
            current = ctx.getParent();
        }
        return testInstances;
    }

    private List<StoreAdapter> findSharedContainers(Class<?> testClass) {
        return ReflectionUtils.findFields(
                testClass,
                isSharedContainer(),
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .map(f -> getContainerInstance(null, f))
            .collect(toList());
    }

    private Predicate<Field> isSharedContainer() {
        return isContainer().and(ReflectionUtils::isStatic);
    }

    private Stream<StoreAdapter> findRestartContainers(Object testInstance) {
        return ReflectionUtils.findFields(
                testInstance.getClass(),
                isRestartContainer(),
                ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .map(f -> getContainerInstance(testInstance, f));
    }

    private Predicate<Field> isRestartContainer() {
        return isContainer().and(ReflectionUtils::isNotStatic);
    }

    private static Predicate<Field> isContainer() {
        return field -> {
            boolean isAnnotatedWithContainer = AnnotationSupport.isAnnotated(field, Container.class);
            if (isAnnotatedWithContainer) {
                boolean isStartable = Startable.class.isAssignableFrom(field.getType());

                if (!isStartable) {
                    throw new ExtensionConfigurationException(String.format("FieldName: %s does not implement Startable", field.getName()));
                }
                return true;
            }
            return false;
        };
    }

    private static StoreAdapter getContainerInstance(final Object testInstance, final Field field) {
        try {
            field.setAccessible(true);
            Startable containerInstance = Preconditions.notNull((Startable) field.get(testInstance), "Container " + field.getName() + " needs to be initialized");
            return new StoreAdapter(field.getDeclaringClass(), field.getName(), containerInstance);
        } catch (IllegalAccessException e) {
            throw new ExtensionConfigurationException("Can not access container defined in field " + field.getName());
        }
    }

    /**
     * An adapter for {@link Startable} that implement {@link CloseableResource}
     * thereby letting the JUnit automatically stop containers once the current
     * {@link ExtensionContext} is closed.
     */
    private static class StoreAdapter implements CloseableResource {

        @Getter
        private String key;

        private Startable container;

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
