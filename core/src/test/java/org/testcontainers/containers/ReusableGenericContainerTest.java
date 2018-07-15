package org.testcontainers.containers;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.containers.ReusableContainerConfiguration.builder;

/**
 * @author Eugeny Karpov
 */
@Slf4j
public class ReusableGenericContainerTest {

    private static final DockerClient dockerClient = DockerClientFactory.instance().client();

    @Test
    public void testCreatingReusableContainer() throws Exception {
        String containerName = "testCreatingReusableContainer-consul";

        stopAndRemoveContainers(containerName);

        try (GenericContainer container = createReusableContainer(containerName)) {
            container.start();

            assertTrue(container.isRunning());
        }
    }

    @Test
    public void reusePreviouslyStoppedContainer() throws Exception {
        String containerName = "reusePreviouslyStoppedContainer-consul";

        stopAndRemoveContainers(containerName);

        try (
            GenericContainer firstContainerObject = createReusableContainer(containerName);
            GenericContainer secondContainerObject = createReusableContainer(containerName)
        ) {
            firstContainerObject.start();
            String firstContainerId = firstContainerObject.getContainerId();

            dockerClient.stopContainerCmd(firstContainerId).exec();
            assertFalse(firstContainerObject.isRunning());

            secondContainerObject.start();
            String secondContainerId = secondContainerObject.getContainerId();

            assertEquals(firstContainerId, secondContainerId);
            assertTrue(secondContainerObject.isRunning());
        }
    }

    @Test
    public void reuseRunningContainer() throws Exception {
        String containerName = "reuseRunningContainer-consul";

        stopAndRemoveContainers(containerName);

        try (
            GenericContainer firstContainerObject = createAndStartReusableContainer(containerName);
            GenericContainer secondContainerObject = createAndStartReusableContainer(containerName)
        ) {
            assertEquals(firstContainerObject.getContainerId(), secondContainerObject.getContainerId());
            assertTrue(secondContainerObject.isRunning());
        }
    }

    @Test
    public void disableReusableStrategy() throws Exception {
        String containerName = "disableReusableStrategy-consul";

        stopAndRemoveContainers(containerName);

        // explicitly disable reusable strategy globally
        TestcontainersConfiguration.getInstance().updateGlobalConfig("containers.reuse", "false");
        try(
            GenericContainer firstContainer = createAndStartReusableContainer(containerName);
            GenericContainer secondContainer = createReusableContainer(containerName);
            ) {
            assertTrue(firstContainer.isRunning());
            assertNotEquals(containerName, firstContainer.getContainerName());

            TestcontainersConfiguration.getInstance().updateGlobalConfig("containers.reuse", "true");

            // explicitly disable reusable strategy for one container
            secondContainer.withReuseExistingContainerStrategy(builder()
                .withContainerName(containerName).isEnabled(false).build());
            secondContainer.start();

            assertTrue(secondContainer.isRunning());
            assertNotEquals(containerName, secondContainer.getContainerName());

            assertNotEquals(firstContainer.getContainerName(), secondContainer.getContainerName());
        } finally {
            TestcontainersConfiguration.getInstance().updateGlobalConfig("containers.reuse", "true");
        }
    }

    @Test(expected = ContainerLaunchException.class)
    public void tryWrongImage() throws Exception {
        String containerName = "tryWrongImage-consul";

        stopAndRemoveContainers(containerName);

        try (
            GenericContainer firstContainerObject = createReusableContainer(containerName, "consul:1.2.1");
            GenericContainer secondContainerObject = createReusableContainer(containerName, "consul:1.2.0")
        ) {
            firstContainerObject.start();

            secondContainerObject.start();
        }
    }

    private GenericContainer createAndStartReusableContainer(String containerName) {
        GenericContainer container = createReusableContainer(containerName);
        container.start();
        return container;
    }

    private GenericContainer createReusableContainer(String containerName) {
        return createReusableContainer(containerName, "consul:1.2.1");
    }

    private GenericContainer createReusableContainer(String containerName, String imageName) {
        return new GenericContainer<>(imageName)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withReuseExistingContainerStrategy(builder()
                .withContainerName(containerName)
                .build());
    }

    private void stopAndRemoveContainers(String containerName) {
        dockerClient.listContainersCmd()
            .withShowAll(true)
            .withNameFilter(Collections.singletonList(containerName))
            .exec()
            .stream()
            .map(Container::getId)
            .forEach(containerId -> {
                if (isContainerRunning(containerId)) {
                    dockerClient.stopContainerCmd(containerId).exec();
                }
                dockerClient.removeContainerCmd(containerId).exec();
            });
    }

    private Boolean isContainerRunning(String containerId) {
        return dockerClient.inspectContainerCmd(containerId).exec().getState().getRunning();
    }
}
