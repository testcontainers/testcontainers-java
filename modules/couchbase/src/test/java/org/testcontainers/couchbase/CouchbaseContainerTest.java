package org.testcontainers.couchbase;

import org.junit.Assert;
import org.junit.Test;

public class CouchbaseContainerTest {

    @Test
    public void shouldUseCorrectDockerImage() {
        CouchbaseContainer couchbaseContainer = new CouchbaseContainer().withClusterAdmin("admin", "foobar");

        Assert.assertEquals(CouchbaseContainer.DOCKER_IMAGE_NAME + CouchbaseContainer.VERSION,
            couchbaseContainer.getDockerImageName());
    }

    @Test
    public void shouldStopWithoutThrowingException() {
        CouchbaseContainer couchbaseContainer = new CouchbaseContainer();
        couchbaseContainer.start();
        couchbaseContainer.stop();
    }
}
