package org.testcontainers;

import com.github.dockerjava.api.exception.NotFoundException;
import org.junit.Test;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.DockerClientFactory.DiskSpaceUsage;
import org.testcontainers.dockerclient.LogToStringContainerCallback;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Test for {@link DockerClientFactory}.
 */
public class DockerClientFactoryTest {

    @Test
    public void runCommandInsideDockerShouldNotFailIfImageDoesNotExistsLocally() {

        final DockerClientFactory dockFactory = DockerClientFactory.instance();
        try {
            //remove tiny image, so it will be pulled during next command run
            dockFactory.client()
                    .removeImageCmd(TestcontainersConfiguration.getInstance().getTinyImage())
                    .withForce(true).exec();
        } catch (NotFoundException ignored) {
            // Do not fail if it's not pulled yet
        }

        dockFactory.runInsideDocker(
                cmd -> cmd.withCmd("sh", "-c", "echo 'SUCCESS'"),
                (client, id) ->
                        client.logContainerCmd(id)
                                .withStdOut(true)
                                .exec(new LogToStringContainerCallback())
                                .toString()
        );
    }

    @Test
    public void shouldHandleBigDiskSize() throws Exception {
        String dfOutput = "/dev/disk1     2982480572 1491240286 2982480572    31%    /";
        DiskSpaceUsage usage = DockerClientFactory.instance().parseAvailableDiskSpace(dfOutput);

        VisibleAssertions.assertEquals("Available MB is correct", 2982480572L / 1024L, usage.availableMB.orElse(0L));
        VisibleAssertions.assertEquals("Available percentage is correct", 31, usage.usedPercent.orElse(0));
    }
}