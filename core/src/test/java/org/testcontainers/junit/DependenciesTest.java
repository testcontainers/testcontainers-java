package org.testcontainers.junit;

import lombok.Getter;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testcontainers.TestImages.TINY_IMAGE;

public class DependenciesTest {

    @Test
    public void shouldWorkWithSimpleDependency() {
        InvocationCountingStartable startable = new InvocationCountingStartable();

        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .dependsOn(startable)
        ) {
            container.start();
        }

        VisibleAssertions.assertEquals("Started once", 1, startable.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("Does not trigger .stop()", 0, startable.getStopInvocationCount().intValue());
    }

    @Test
    public void shouldWorkWithMutlipleDependencies() {
        InvocationCountingStartable startable1 = new InvocationCountingStartable();
        InvocationCountingStartable startable2 = new InvocationCountingStartable();

        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .dependsOn(startable1, startable2)
        ) {
            container.start();
        }

        VisibleAssertions.assertEquals("Startable1 started once", 1, startable1.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("Startable2 started once", 1, startable2.getStartInvocationCount().intValue());
    }

    @Test
    public void shouldStartEveryTime() {
        InvocationCountingStartable startable = new InvocationCountingStartable();

        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .dependsOn(startable)
        ) {
            container.start();
            container.stop();

            container.start();
            container.stop();

            container.start();
        }

        VisibleAssertions.assertEquals("Started multiple times", 3, startable.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("Does not trigger .stop()", 0, startable.getStopInvocationCount().intValue());
    }

    @Test
    public void shouldStartTransitiveDependencies() {
        InvocationCountingStartable transitiveOfTransitiveStartable = new InvocationCountingStartable();
        InvocationCountingStartable transitiveStartable = new InvocationCountingStartable();
        transitiveStartable.getDependencies().add(transitiveOfTransitiveStartable);

        InvocationCountingStartable startable = new InvocationCountingStartable();
        startable.getDependencies().add(transitiveStartable);

        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .dependsOn(startable)
        ) {
            container.start();
            container.stop();
        }

        VisibleAssertions.assertEquals("Root started", 1, startable.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("Transitive started", 1, transitiveStartable.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("Transitive of transitive started", 1, transitiveOfTransitiveStartable.getStartInvocationCount().intValue());
    }

    @Test
    public void shouldHandleDiamondDependencies() throws Exception {
        InvocationCountingStartable a = new InvocationCountingStartable();
        InvocationCountingStartable b = new InvocationCountingStartable();
        InvocationCountingStartable c = new InvocationCountingStartable();
        InvocationCountingStartable d = new InvocationCountingStartable();
        //  / b \
        // a     d
        //  \ c /
        b.getDependencies().add(a);
        c.getDependencies().add(a);

        d.getDependencies().add(b);
        d.getDependencies().add(c);

        Startables.deepStart(Stream.of(d)).get(1, TimeUnit.SECONDS);

        VisibleAssertions.assertEquals("A started", 1, a.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("B started", 1, b.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("C started", 1, c.getStartInvocationCount().intValue());
        VisibleAssertions.assertEquals("D started", 1, d.getStartInvocationCount().intValue());
    }

    @Test
    public void shouldHandleParallelStream() throws Exception {
        List<Startable> startables = Stream.generate(InvocationCountingStartable::new)
            .limit(10)
            .collect(Collectors.toList());

        for (int i = 1; i < startables.size(); i++) {
            startables.get(0).getDependencies().add(startables.get(i));
        }

        Startables.deepStart(startables.parallelStream()).get(1, TimeUnit.SECONDS);
    }

    private static class InvocationCountingStartable implements Startable {

        @Getter
        Set<Startable> dependencies = new HashSet<>();

        @Getter
        AtomicLong startInvocationCount = new AtomicLong(0);

        @Getter
        AtomicLong stopInvocationCount = new AtomicLong(0);

        @Override
        public void start() {
            startInvocationCount.getAndIncrement();
        }

        @Override
        public void stop() {
            stopInvocationCount.getAndIncrement();
        }
    }
}
