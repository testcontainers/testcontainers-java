package org.testcontainers.memcached;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class MemcachedContainer extends GenericContainer<MemcachedContainer> {

    private static final int DEFAULT_PORT = 11211;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("memcached");

    public MemcachedContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public MemcachedContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(DEFAULT_PORT);
        waitingFor(Wait.forListeningPort());
    }

    public String getHostPort() {
        return getHost() + ":" + getMappedPort(DEFAULT_PORT);
    }
}
