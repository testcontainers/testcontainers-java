package org.testcontainers.junit;

import org.apache.commons.compress.utils.Sets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

@RunWith(Parameterized.class)
public class DockerComposeContainerWithServicesTest {

    private final String[] services;
    private final String[] exposedServices;
    private final String[] waitForServices;
    private final String[] scaledServices;
    private final String[] expected;

    public DockerComposeContainerWithServicesTest(final String[] services,
                                                  final String[] exposedServices,
                                                  final String[] waitForServices,
                                                  final String[] scaledServices,
                                                  final String[] expected) {
        this.services = services;
        this.exposedServices = exposedServices;
        this.waitForServices = waitForServices;
        this.scaledServices = scaledServices;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static Collection params() {
        return Arrays.asList(new String[][][] {
                {{}, {}, {}, {}, {"redis", "db"}},
                {{}, {"redis"}, {"redis"}, {"db"}, {"redis", "db"}},
                {{"redis"}, {}, {}, {}, {"redis"}},
                {{"redis"}, {"redis"}, {"redis"}, {"db"}, {"redis", "db"}},
                {{"db"}, {}, {}, {}, {"db"}},
                {{"db"}, {"redis"}, {}, {"db"}, {"redis", "db"}},
                {{"db"}, {}, {"redis"}, {"db"}, {"redis", "db"}},
                {{"redis", "db"}, {}, {}, {}, {"redis", "db"}},
                {{"redis", "db"}, {"redis"}, {"redis"}, {"db"}, {"redis", "db"}}
        });
    }

    private static final int REDIS_PORT = 6379;

    @Test
    public void testGivenDockerCompose_WhenSublistOfServicesSelected_ThenOnlyThoseServicesAreStarted() {
        try (DockerComposeContainer<?> environment = createEnvironment()) {
            environment.start();
            Set<String> runningServices = environment.listChildContainers()
                    .stream()
                    .map(environment::getServiceNameFromContainer)
                    .map(environment::getServiceName)
                    .collect(Collectors.toSet());
            assertEquals("The actual running services should match expectation", Sets.newHashSet(expected), runningServices);
        }
    }

    private DockerComposeContainer createEnvironment() {
        DockerComposeContainer baseContainer = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
                .withServices(services)
                .withLogConsumer("redis", outputFrame -> System.out.println(outputFrame.toString()))
                .withLogConsumer("db", outputFrame -> System.out.println(outputFrame.toString()));
        Arrays.stream(exposedServices).forEach(x -> baseContainer.withExposedService(x, REDIS_PORT));
        Arrays.stream(waitForServices).forEach(x -> baseContainer.waitingFor(x, Wait.defaultWaitStrategy()));
        Arrays.stream(scaledServices).forEach(x -> baseContainer.withScaledService(x, 1));
        return baseContainer;
    }
}