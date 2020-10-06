package org.testcontainers.junit.jqwik;

import net.jqwik.api.JqwikException;
import net.jqwik.api.lifecycle.AroundContainerHook;
import net.jqwik.api.lifecycle.AroundPropertyHook;
import net.jqwik.api.lifecycle.AroundTryHook;
import net.jqwik.api.lifecycle.ContainerLifecycleContext;
import net.jqwik.api.lifecycle.LifecycleContext;
import net.jqwik.api.lifecycle.Lifespan;
import net.jqwik.api.lifecycle.PropertyExecutionResult;
import net.jqwik.api.lifecycle.PropertyExecutor;
import net.jqwik.api.lifecycle.PropertyLifecycleContext;
import net.jqwik.api.lifecycle.SkipExecutionHook;
import net.jqwik.api.lifecycle.Store;
import net.jqwik.api.lifecycle.TryExecutionResult;
import net.jqwik.api.lifecycle.TryExecutor;
import net.jqwik.api.lifecycle.TryLifecycleContext;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.commons.util.ReflectionUtils;
import org.opentest4j.TestAbortedException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class TestcontainersExtension implements AroundTryHook, AroundPropertyHook, AroundContainerHook, SkipExecutionHook {

    private static final Object IDENTIFIER = TestcontainersExtension.class;
    private static final Object SHARED_LIFECYCLE_AWARE_TEST_CONTAINERS = new Object();

    @Override
    public void beforeContainer(ContainerLifecycleContext context) {
        Class<?> testClass = context.optionalContainerClass().orElseThrow(() -> new IllegalStateException("TestcontainersExtension is only supported for classes."));
        Store<List<Startable>> store = getOrCreateContainerClosingStore(IDENTIFIER, Lifespan.RUN);

        List<TestLifecycleAware> lifecycleAwareContainers = startContainersAndFindLifeCycleAwareOnes(store, findSharedContainers(testClass));

        Store.getOrCreate(SHARED_LIFECYCLE_AWARE_TEST_CONTAINERS, Lifespan.RUN, () -> lifecycleAwareContainers);
        signalBeforeTestToContainers(lifecycleAwareContainers, testDescriptionFrom(context));
    }

    @Override
    public void afterContainer(ContainerLifecycleContext context) {
        Store<List<TestLifecycleAware>> containers = Store.getOrCreate(SHARED_LIFECYCLE_AWARE_TEST_CONTAINERS, Lifespan.RUN, ArrayList::new);
        signalAfterTestToContainersFor(containers.get(), testDescriptionFrom(context));
    }

    @Override
    public int proximity() {
        // must be run before the @BeforeContainer annotation and after @AfterContainer annotation
        return -11;
    }

    @Override
    public PropertyExecutionResult aroundProperty(PropertyLifecycleContext context, PropertyExecutor property) {
        Object testInstance = context.testInstance();
        Store<List<Startable>> store = getOrCreateContainerClosingStore(property.hashCode(), Lifespan.PROPERTY);

        List<TestLifecycleAware> lifecycleAwareContainers = startContainersAndFindLifeCycleAwareOnes(store, findRestartContainers(testInstance));

        TestDescription testDescription = testDescriptionFrom(context);
        signalBeforeTestToContainers(lifecycleAwareContainers, testDescription);
        PropertyExecutionResult executionResult = property.execute();
        signalAfterTestToContainersFor(lifecycleAwareContainers, testDescription, executionResult);

        return executionResult;
    }

    @Override
    public int aroundPropertyProximity() {
        return -11; // Run before BeforeProperty and after AfterProperty
    }

    @Override
    public TryExecutionResult aroundTry(TryLifecycleContext context, TryExecutor aTry, List<Object> parameters) throws Throwable {
        Object testInstance = context.testInstance();
        Store<List<Startable>> store = getOrCreateContainerClosingStore(aTry.hashCode(), Lifespan.TRY);

        startContainersAndFindLifeCycleAwareOnes(store, findRestartContainersPerTry(testInstance));
        return aTry.execute(parameters);
    }

    @Override
    public int aroundTryProximity() {
        return -11;
    }

    private Store<List<Startable>> getOrCreateContainerClosingStore(Object identifier, Lifespan lifespan) {
        Store<List<Startable>> store = Store.getOrCreate(identifier, lifespan, ArrayList::new);
        store.onClose(startables -> startables.forEach(Startable::close));
        return store;
    }

    private List<TestLifecycleAware> startContainersAndFindLifeCycleAwareOnes(Store<List<Startable>> store, Stream<Startable> containers) {
        return containers
            .peek(startable -> store.update(startables -> {
                List<Startable> update = new ArrayList<>(startables);
                startable.start();
                update.add(startable);
                return update;
            }))
            .filter(this::isTestLifecycleAware)
            .map(startable -> (TestLifecycleAware) startable)
            .collect(toList());
    }

    private void signalBeforeTestToContainers(List<TestLifecycleAware> lifecycleAwareContainers, TestDescription testDescription) {
        lifecycleAwareContainers.forEach(container -> container.beforeTest(testDescription));
    }

    private void signalAfterTestToContainersFor(List<TestLifecycleAware> containers, TestDescription testDescription){
        containers.forEach(container -> container.afterTest(testDescription, Optional.empty()));
    }

    private void signalAfterTestToContainersFor(List<TestLifecycleAware> containers, TestDescription testDescription, PropertyExecutionResult executionResult) {
        containers.forEach(container -> {
            if(executionResult.status() == PropertyExecutionResult.Status.ABORTED){
                container.afterTest(testDescription, Optional.of(new TestAbortedException()));
            } else {
                container.afterTest(testDescription, executionResult.throwable());
            }
        });
    }

    private TestDescription testDescriptionFrom(LifecycleContext context) {
        return new TestcontainersTestDescription(
            context.label(),
            FilesystemFriendlyNameGenerator.filesystemFriendlyNameOf(context)
        );
    }

    private boolean isTestLifecycleAware(Startable startable) {
        return startable instanceof TestLifecycleAware;
    }

    @Override
    public SkipResult shouldBeSkipped(LifecycleContext context) {
        return findTestcontainers(context)
            .map(this::evaluateSkipResult)
            .orElseThrow(() -> new JqwikException("@Testcontainers not found"));
    }

    private Optional<Testcontainers> findTestcontainers(LifecycleContext context) {
        // Find closest TestContainers annotation
        Optional<Testcontainers> first = context.findAnnotationsInContainer(Testcontainers.class).stream().findFirst();
        if(first.isPresent())
            return first;
        else
            return context.findAnnotation(Testcontainers.class);
    }

    private SkipResult evaluateSkipResult(Testcontainers testcontainers) {
        if (testcontainers.disabledWithoutDocker()) {
            if (isDockerAvailable()) {
                return SkipResult.doNotSkip();
            }
            return SkipResult.skip("disabledWithoutDocker is true and Docker is not available");
        }
        return SkipResult.doNotSkip();
    }

    boolean isDockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private Stream<Startable> findSharedContainers(Class<?> testClass) {
        final Predicate<Field> isSharedContainer = ReflectionUtils::isStatic;
        return findContainers(null, isSharedContainer, testClass);
    }

    private Stream<Startable> findRestartContainers(Object testInstance) {
        final Predicate<Field> isRestartContainer = ReflectionUtils::isNotStatic;
        return findContainers(testInstance, isRestartContainer.and(restartPerTry().negate()), testInstance.getClass());
    }

    private Stream<Startable> findRestartContainersPerTry(Object testInstance) {
        final Predicate<Field> isRestartContainer = ReflectionUtils::isNotStatic;
        return findContainers(testInstance, isRestartContainer.and(restartPerTry()), testInstance.getClass());
    }

    private Stream<Startable> findContainers(Object testInstance, Predicate<Field> containerCondition, Class<?> testClass) {
        return ReflectionUtils.findFields(
            testClass,
            isContainer().and(containerCondition),
            ReflectionUtils.HierarchyTraversalMode.TOP_DOWN)
            .stream()
            .map(f -> getContainerInstance(testInstance, f));
    }

    private static Predicate<Field> restartPerTry() {
        return field -> AnnotationSupport.findAnnotation(field, Container.class)
            .filter(Container::restartPerTry)
            .isPresent();
    }

    private static Predicate<Field> isContainer() {
        return field -> {
            boolean isAnnotatedWithContainer = AnnotationSupport.isAnnotated(field, Container.class);
            if (isAnnotatedWithContainer) {
                boolean isStartable = Startable.class.isAssignableFrom(field.getType());

                if (!isStartable) {
                    throw new JqwikException(String.format("FieldName: %s does not implement Startable", field.getName()));
                }
                return true;
            }
            return false;
        };
    }

    private static Startable getContainerInstance(final Object testInstance, final Field field) {
        try {
            field.setAccessible(true);
            return Preconditions.notNull((Startable) field.get(testInstance), "Container " + field.getName() + " needs to be initialized");
        } catch (IllegalAccessException e) {
            throw new JqwikException("Can not access container defined in field " + field.getName());
        }
    }
}
