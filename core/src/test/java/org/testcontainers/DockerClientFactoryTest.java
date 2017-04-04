package org.testcontainers;

import org.junit.Test;
import org.testcontainers.dockerclient.LogToStringContainerCallback;
import org.testcontainers.utility.TestcontainersConfiguration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created mgorelikov on 05/04/2017
 * Test for {@link DockerClientFactory}.
 */
public class DockerClientFactoryTest {

  private static final String SUCCESS = "SUCCESS";

  @Test
  public void runCommandInsideDockerShouldPullImageIfItDoesNotExistsLocally() {

    final DockerClientFactory dockFactory = DockerClientFactory.instance();
    final String imageName = TestcontainersConfiguration.getInstance().getTinyImage();
    //remove tiny image, so it will be pulled during next command run
    dockFactory.client()
        .removeImageCmd(imageName)
        .withForce(true).exec();

    String result = dockFactory.runInsideDocker(
        cmd -> cmd.withCmd("sh", "-c", "echo '" + SUCCESS + "'"),
        (client, id) ->
            client.logContainerCmd(id)
                .withStdOut(true)
                .exec(new LogToStringContainerCallback())
                .toString()
    );
    //check local image availability
    assertTrue(isImageAvailable(dockFactory, imageName));
    assertEquals(SUCCESS + '\n', result);
  }

  private boolean isImageAvailable(DockerClientFactory dockFactory, String imageName) {
    return !dockFactory.client().listImagesCmd()
          .withImageNameFilter(imageName)
          .exec().isEmpty();
  }
}