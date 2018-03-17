package org.testcontainers.junit5;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.extension.*;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.util.*;

@Slf4j
public class TestcontainersExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

    private static final Set<Startable> SINGLETONS = new HashSet<>();

    private final List<Startable> perClassContainers = new ArrayList<>();

    private final List<Startable> perTestContainers = new ArrayList<>();

    private volatile boolean beforeAllHappened = false;

    private volatile boolean beforeEachHappened = false;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        beforeAllHappened = true;

        Startables.deepStart(SINGLETONS).get();
        start(toDescription(context), perClassContainers);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        beforeEachHappened = true;
        if (!beforeAllHappened && !perClassContainers.isEmpty()) {
            throw new IllegalStateException("You have per-class containers defined but TestcontainersExtension is not static");
        }

        start(toDescription(context), perTestContainers);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        stop(toDescription(context), context.getExecutionException(), perTestContainers);
        perTestContainers.clear();
    }

    @Override
    public void afterAll(ExtensionContext context) {
        stop(toDescription(context), context.getExecutionException(), perClassContainers);
    }

    public <T extends Startable> T singleton(T container) {
        if (beforeEachHappened) {
            throw new IllegalStateException("beforeEach() already happened! Did you forget to use static?");
        }

        SINGLETONS.add(container);
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

    protected void start(TestDescription description, Collection<Startable> startables) throws Exception {
        for (Startable startable : startables) {
            if (startable instanceof TestLifecycleAware) {
                ((TestLifecycleAware) startable).beforeTestBlock(description);
            }
        }

        Startables.deepStart(startables).get();
    }

    protected void stop(TestDescription description, Optional<Throwable> throwable, Collection<Startable> startables) {
        startables.parallelStream().forEach(it -> {
            if (it instanceof TestLifecycleAware) {
                ((TestLifecycleAware) it).afterTestBlock(description, throwable);
            }
            it.stop();
        });
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
