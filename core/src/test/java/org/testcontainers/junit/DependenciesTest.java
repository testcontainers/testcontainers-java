package org.testcontainers.junit;

import lombok.Getter;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.lifecycle.Startables;

import com.google.common.util.concurrent.Uninterruptibles;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

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
    public void lazyEnvVars() throws Exception {
        GenericContainer<?> containerA = new GenericContainer<>("alpine:3.2")
                .withExposedPorts(80)
                .withEnv("MAGIC_NUMBER", "4")
                .withCommand("/bin/sh", "-c", "while true; do echo \"$MAGIC_NUMBER\" | nc -l -p 80; done");
        
        GenericContainer<?> containerB = new GenericContainer<>("alpine:3.2")
                .withExposedPorts(80)
                .withEnv("CONTAINER_A_PORT", () -> "" + containerA.getMappedPort(80))
                .withCommand("/bin/sh", "-c", "while true; do echo \"$CONTAINER_A_PORT\" | nc -l -p 80; done")
                .dependsOn(containerA);
        
        containerB.start();
        String containerBResponse = getReaderForContainerPort80(containerB).readLine();
        VisibleAssertions.assertEquals("Container B could read container A's exposed port", 
                "" + containerA.getMappedPort(80), containerBResponse);
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
    
    private BufferedReader getReaderForContainerPort80(GenericContainer container) {

        return Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            Socket socket = new Socket(container.getContainerIpAddress(), container.getFirstMappedPort());
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        });
    }
}
