package org.testcontainers;

import org.junit.Test;
import org.testcontainers.dockerclient.LogToStringContainerCallback;
import org.testcontainers.utility.TestcontainersConfiguration;

/**
 * Test for {@link DockerClientFactory}.
 */
public class DockerClientFactoryTest {

  @Test
  public void runCommandInsideDockerShouldNotFailIfImageDoesNotExistsLocally() {

    final DockerClientFactory dockFactory = DockerClientFactory.instance();
    //remove tiny image, so it will be pulled during next command run
    dockFactory.client()
        .removeImageCmd(TestcontainersConfiguration.getInstance().getTinyImage())
        .withForce(true).exec();

    dockFactory.runInsideDocker(
        cmd -> cmd.withCmd("sh", "-c", "echo 'SUCCESS'"),
        (client, id) ->
            client.logContainerCmd(id)
                .withStdOut(true)
                .exec(new LogToStringContainerCallback())
                .toString()
    );
  }
}