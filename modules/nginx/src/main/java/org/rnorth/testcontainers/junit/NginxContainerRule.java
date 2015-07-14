package org.rnorth.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.rnorth.testcontainers.containers.NginxContainer;
import org.rnorth.testcontainers.containers.traits.LinkableContainer;
import org.rnorth.testcontainers.containers.traits.LinkableContainerRule;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author richardnorth
 */
public class NginxContainerRule extends ExternalResource implements LinkableContainerRule {

    private final NginxContainer container;

    public NginxContainerRule() {
        container = new NginxContainer();
    }

    @Override
    protected void before() throws Throwable {
        container.start();
    }

    @Override
    protected void after() {
        container.stop();
    }

    public NginxContainerRule withCustomContent(String htmlContentPath) {
        container.setCustomConfig(htmlContentPath);
        return this;
    }

    public URL getBaseUrl(String scheme, int internalPort) throws MalformedURLException {
        return container.getBaseUrl(scheme, internalPort);
    }

    public NginxContainerRule withExposedPorts(String... ports) {
        container.setExposedPorts(ports);
        return this;
    }

    @Override
    public LinkableContainer getContainer() {
        return this.container;
    }
}
