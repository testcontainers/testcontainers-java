package com.example.linkedcontainer;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * A Redmine container.
 */
public class RedmineContainer extends GenericContainer<RedmineContainer> {

    private static final int REDMINE_PORT = 3000;

    public RedmineContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        addExposedPort(REDMINE_PORT);
        waitingFor(Wait.forHttp("/"));
    }

    public RedmineContainer withLinkToContainer(LinkableContainer otherContainer, String alias) {
        addLink(otherContainer, alias);
        return this;
    }

    public String getRedmineUrl() {
        return String.format("http://%s:%d",
                this.getContainerIpAddress(),
                this.getMappedPort(REDMINE_PORT));
    }
}
