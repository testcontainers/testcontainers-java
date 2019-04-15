package org.testcontainers.junit;

import lombok.Getter;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.lifecycle.Startable;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class DependenciesTest {

    @Test
    public void shouldWorkWithSimpleDependency() {
        InvocationCountingStartable startable = new InvocationCountingStartable();

        try (
            GenericContainer container = new GenericContainer()
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
            GenericContainer container = new GenericContainer()
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
            GenericContainer container = new GenericContainer()
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
            GenericContainer container = new GenericContainer()
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
