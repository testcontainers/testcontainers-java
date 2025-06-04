package org.testcontainers.junit.vintage;

import org.junit.platform.commons.support.AnnotationSupport;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ModifierSupport;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junit.runner.Description;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Integrates Testcontainers with the JUnit4 lifecycle.
 */
public final class Testcontainers extends FailureDetectingExternalResource {

    private final Object testInstance;

    private List<Startable> startedContainers = Collections.emptyList();

    private List<TestLifecycleAware> lifecycleAwareContainers = Collections.emptyList();

    /**
     * Constructs an instance for use by {@code @Rule}.
     *
     * @param testInstance instance of the current test.
     */
    public Testcontainers(Object testInstance) {
        this.testInstance = Objects.requireNonNull(testInstance);
    }

    /**
     * Constructs an instance for use by {@code @ClassRule}.
     */
    public Testcontainers() {
        testInstance = null;
    }

    @Override
    protected void starting(Description description) {
        if (description.isTest()) {
            if (testInstance == null) {
                throw new RuntimeException("Testcontainers used as a @Rule without being provided a test instance");
            }
        } else if (testInstance != null) {
            throw new RuntimeException("Testcontainers used as a @ClassRule but was provided a test instance");
        }

        List<Startable> containers = findContainers(description);
        startedContainers = new ArrayList<>(containers.size());
        containers.forEach(startable -> {
            startable.start();
            startedContainers.add(startable);
        });

        lifecycleAwareContainers =
            startedContainers
                .stream()
                .filter(startable -> startable instanceof TestLifecycleAware)
                .map(TestLifecycleAware.class::cast)
                .collect(Collectors.toList());
        if (!lifecycleAwareContainers.isEmpty()) {
            TestDescription testDescription = toTestDescription(description);
            lifecycleAwareContainers.forEach(container -> container.beforeTest(testDescription));
        }
    }

    @Override
    protected void succeeded(Description description) {
        if (!lifecycleAwareContainers.isEmpty()) {
            TestDescription testDescription = toTestDescription(description);
            forEachReversed(
                lifecycleAwareContainers,
                container -> container.afterTest(testDescription, Optional.empty())
            );
        }
    }

    @Override
    protected void failed(Throwable e, Description description) {
        if (!lifecycleAwareContainers.isEmpty()) {
            TestDescription testDescription = toTestDescription(description);
            Optional<Throwable> exception = Optional.of(e);
            forEachReversed(lifecycleAwareContainers, container -> container.afterTest(testDescription, exception));
        }
    }

    @Override
    protected void finished(Description description, List<Throwable> errors) {
        forEachReversed(
            startedContainers,
            startable -> {
                try {
                    startable.stop();
                } catch (Throwable e) {
                    errors.add(e);
                }
            }
        );
    }

    private List<Startable> findContainers(Description description) {
        if (description.getTestClass() == null) {
            return Collections.emptyList();
        }
        Predicate<Field> isTargetedContainerField = isContainerField();
        if (testInstance == null) {
            isTargetedContainerField = isTargetedContainerField.and(ModifierSupport::isStatic);
        } else {
            isTargetedContainerField = isTargetedContainerField.and(ModifierSupport::isNotStatic);
        }

        return ReflectionSupport
            .streamFields(description.getTestClass(), isTargetedContainerField, HierarchyTraversalMode.TOP_DOWN)
            .map(this::getContainerInstance)
            .collect(Collectors.toList());
    }

    private static Predicate<Field> isContainerField() {
        return field -> {
            boolean isAnnotatedWithContainer = AnnotationSupport.isAnnotated(field, Container.class);
            if (isAnnotatedWithContainer) {
                boolean isStartable = Startable.class.isAssignableFrom(field.getType());

                if (!isStartable) {
                    throw new RuntimeException(
                        String.format("The @Container field '%s' does not implement Startable", field.getName())
                    );
                }
                return true;
            }
            return false;
        };
    }

    private Startable getContainerInstance(Field field) {
        try {
            field.setAccessible(true);
            Startable containerInstance = (Startable) field.get(testInstance);
            if (containerInstance == null) {
                throw new RuntimeException("Container " + field.getName() + " needs to be initialized");
            }
            return containerInstance;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Cannot access container defined in field " + field.getName());
        }
    }

    private static <T> void forEachReversed(List<T> list, Consumer<? super T> callback) {
        ListIterator<T> iterator = list.listIterator(list.size());
        while (iterator.hasPrevious()) {
            callback.accept(iterator.previous());
        }
    }
}
