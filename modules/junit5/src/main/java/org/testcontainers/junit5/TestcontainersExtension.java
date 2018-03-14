package org.testcontainers.junit5;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.*;

@Slf4j
public class TestcontainersExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Map<Startable, Boolean> singletons = new HashMap<>();

    private final List<Startable> perClassContainers = new ArrayList<>();

    private final List<Startable> perTestContainers = new ArrayList<>();

    private volatile boolean beforeAllHappened = false;

    private volatile boolean beforeEachHappened = false;

    protected void startSingletons() {
        singletons.replaceAll((container, started) -> {
            if (!started) {
                container.start();
            }

            return true;
        });
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        beforeAllHappened = true;
        startSingletons();

        for (Startable perClassContainer : perClassContainers) {
            if (perClassContainer instanceof TestLifecycleAware) {
                ((TestLifecycleAware) perClassContainer).beforeTestBlock(toDescription(context));
            }
            perClassContainer.start();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        beforeEachHappened = true;
        if (!beforeAllHappened && !perClassContainers.isEmpty()) {
            throw new IllegalStateException("You have per-class containers defined but TestcontainersExtension is not static");
        }

        startSingletons();

        for (Startable perTestContainer : perTestContainers) {
            if (perTestContainer instanceof TestLifecycleAware) {
                ((TestLifecycleAware) perTestContainer).beforeTestBlock(toDescription(context));
            }
            perTestContainer.start();
        }
    }

    @Override
    public void afterEach(ExtensionContext context) {
        for (Startable perTestContainer : perTestContainers) {
            if (perTestContainer instanceof TestLifecycleAware) {
                ((TestLifecycleAware) perTestContainer).afterTestBlock(toDescription(context), context.getExecutionException());
            }
            perTestContainer.close();
        }
        perTestContainers.clear();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        for (Startable perClassContainer : perClassContainers) {
            if (perClassContainer instanceof TestLifecycleAware) {
                ((TestLifecycleAware) perClassContainer).afterTestBlock(toDescription(context), context.getExecutionException());
            }
            perClassContainer.close();
        }
    }

    public <T extends Startable> T singleton(T container) {
        if (beforeEachHappened) {
            throw new IllegalStateException("beforeEach() already happened! Did you forget to use static?");
        }

        singletons.put(container, false);
        return container;
    }

    public <T extends Startable> T perClass(T container) {
        if (beforeEachHappened) {
            throw new IllegalStateException("beforeEach() already happened! Did you forget to use static?");
        }

        perClassContainers.add(container);
        return container;
    }

    public <T extends Startable> T perTest(T container) {
        perTestContainers.add(container);
        return container;
    }

    public static TestDescription toDescription(ExtensionContext context) {
        return new TestDescription() {

            @Override
            public String getTestId() {
                return context.getUniqueId();
            }

            @Override
            public String getDisplayName() {
                return context.getDisplayName();
            }

            @Override
            public Optional<String[]> getNameParts() {
                List<String> result = new ArrayList<>();
                context.getTestClass().ifPresent(it -> result.add(it.getSimpleName()));
                context.getTestMethod().ifPresent(it -> result.add(it.getName()));

                return Optional.of(result.toArray(new String[0])).filter(it -> it.length > 0);
            }

            @Override
            public Optional<String> getFilesystemFriendlyName() {
                return getNameParts().map(it -> String.join("-", it));
            }
        };
    }
}
