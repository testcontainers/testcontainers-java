package org.testcontainers.images;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.testcontainers.utility.Base58;

public class RemoteDockerImageTest {

    @Test
    public void toStringContainsImageName() {
        String imageName = Base58.randomString(8).toLowerCase();
        RemoteDockerImage remoteDockerImage = new RemoteDockerImage(imageName);
        assertThat(remoteDockerImage.toString(), containsString(imageName));
    }
}
