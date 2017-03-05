package com.example.linkedcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.Wait;

/**
 * A linkable Redmine container.
 */
public class RedmineContainer<SELF extends RedmineContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {

    private static final String IMAGE = "redmine";
    private static final int REDMINE_PORT = 3000;

    public RedmineContainer() {
        this(IMAGE + ":latest");
    }

    public RedmineContainer(String dockerImageName) {
        super.setDockerImageName(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(REDMINE_PORT);
    }

    @Override
    protected void configure() {
        addExposedPort(REDMINE_PORT);
        waitingFor(Wait.forHttp("/"));
    }

    @Override
    public SELF withEnv(String key, String value) {
        return super.withEnv(key, value);
    }

    public SELF withLinkToContainer(LinkableContainer otherContainer, String alias) {
        addLink(otherContainer, alias);
        return self();
    }

    public String getRedmineUrl() {
        return String.format("http://%s:%d",
                this.getContainerIpAddress(),
                this.getMappedPort(REDMINE_PORT));
    }
}
