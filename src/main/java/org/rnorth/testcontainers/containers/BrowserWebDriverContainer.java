package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.rnorth.testcontainers.containers.traits.LinkableContainer;
import org.rnorth.testcontainers.containers.traits.VncService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A chrome/firefox/custom container based on SeleniumHQ's standalone container sets.
 *
 * The container should expose Selenium remote control protocol and VNC.
 */
public class BrowserWebDriverContainer extends AbstractContainer implements VncService, LinkableContainer {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:2.45.0";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:2.45.0";
    private static final String DEFAULT_PASSWORD = "secret";

    private Map<String, List<PortBinding>> ports;
    private DesiredCapabilities desiredCapabilities;
    private String imageName = null;
    private String seleniumPort;
    private String vncPort;

    /**
     * @param imageName custom image name to use for the container
     */
    public BrowserWebDriverContainer(String imageName) {
        this.imageName = imageName;
    }

    /**
     * Create a container with desired capabilities. The browser type will be used to select a suitable container image.
     *
     * @param desiredCapabilities desired capabilities of the VM, e.g. DesiredCapabilities.chrome() or DesiredCapabilities.firefox()
     */
    public BrowserWebDriverContainer(DesiredCapabilities desiredCapabilities) {
        this.desiredCapabilities = desiredCapabilities;
    }

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        ports = containerInfo.networkSettings().ports();
        seleniumPort = ports.get("4444/tcp").get(0).hostPort();
        vncPort = ports.get("5900/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return seleniumPort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        String timeZone = Optional.of(System.getProperty("user.timezone")).orElse("Etc/UTC");

        return ContainerConfig.builder()
                .image(getDockerImageName())
                .exposedPorts("4444", "5900")
                .env("TZ=" + timeZone)
                .cmd("/opt/bin/entry_point.sh")
                .build();
    }

    @Override
    protected String getDockerImageName() {

        if (imageName != null) {
            return imageName;
        }

        String browserName = desiredCapabilities.getBrowserName();
        switch (browserName) {
            case BrowserType.CHROME:
                return CHROME_IMAGE;
            case BrowserType.FIREFOX:
                return FIREFOX_IMAGE;
            default:
                throw new UnsupportedOperationException("Browser name must be 'chrome' or 'firefox'; provided '" + browserName + "' is not supported");
        }
    }

    public URL getSeleniumAddress() throws MalformedURLException {
        return new URL("http", dockerHostIpAddress, Integer.valueOf(this.seleniumPort), "/wd/hub");
    }

    @Override
    public String getVncAddress() {
        return "vnc://vnc:secret@" + dockerHostIpAddress + ":" + this.vncPort;
    }

    @Override
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    @Override
    public String getContainerId() {
        return containerId;
    }
}
