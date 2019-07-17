package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.traits.LinkableContainer;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Set;

/**
 * @author richardnorth
 */
public class NginxContainer<SELF extends NginxContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {

    public static final int NGINX_DEFAULT_PORT = 80;
    private static final String NGINX_VERSION_STRING = "nginx:1.9.4";

    public NginxContainer() {
        super(NGINX_VERSION_STRING);
    }

    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        return Collections.singleton(getMappedPort(NGINX_DEFAULT_PORT));
    }

    @Override
    protected void configure() {
        addExposedPort(NGINX_DEFAULT_PORT);
        setCommand("nginx", "-g", "daemon off;");
    }

    public URL getBaseUrl(String scheme, int port) throws MalformedURLException {
        return new URL(scheme + "://" + getContainerIpAddress() + ":" + getMappedPort(port));
    }

    public void setCustomContent(String htmlContentPath) {
        addFileSystemBind(htmlContentPath, "/usr/share/nginx/html", BindMode.READ_ONLY);
    }

    public SELF withCustomContent(String htmlContentPath) {
        this.setCustomContent(htmlContentPath);
        return self();
    }
}
