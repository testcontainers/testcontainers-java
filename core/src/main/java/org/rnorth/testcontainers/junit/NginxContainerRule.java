package org.rnorth.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.rnorth.testcontainers.containers.NginxContainer;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author richardnorth
 */
public class NginxContainerRule extends ExternalResource {

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

    public NginxContainerRule withCustomConfig(String htmlContentPath) {
        container.setCustomConfig(htmlContentPath);
        return this;
    }

    public URL getBaseUrl(String scheme, int internalPort) throws MalformedURLException {
        return container.getBaseUrl(scheme, internalPort);
    }
}
