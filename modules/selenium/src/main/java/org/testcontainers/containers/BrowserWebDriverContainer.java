package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import com.spotify.docker.client.messages.PortBinding;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.traits.VncService;
import org.testcontainers.utility.Retryables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A chrome/firefox/custom container based on SeleniumHQ's standalone container sets.
 *
 * The container should expose Selenium remote control protocol and VNC.
 */
public class BrowserWebDriverContainer extends AbstractContainer implements VncService, LinkableContainer {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:2.45.0";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:2.45.0";
    private static final String DEFAULT_PASSWORD = "secret";

    private DesiredCapabilities desiredCapabilities;
    private Map<String, LinkableContainer> containersToLink = Collections.emptyMap();
    private String imageName = null;
    private String seleniumPort;
    private String vncPort;

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebDriverContainer.class);

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

    public BrowserWebDriverContainer(DesiredCapabilities desiredCapabilities, Map<String, LinkableContainer> containersToLink) {
        this.desiredCapabilities = desiredCapabilities;
        this.containersToLink = containersToLink;
    }

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        Map<String, List<PortBinding>> ports = containerInfo.networkSettings().ports();
        seleniumPort = ports.get("4444/tcp").get(0).hostPort();
        vncPort = ports.get("5900/tcp").get(0).hostPort();
    }

    @Override
    protected String getLivenessCheckPort() {
        return seleniumPort;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        String timeZone = System.getProperty("user.timezone");

        if(timeZone == null || timeZone.isEmpty()) {
            timeZone = "Etc/UTC";
        }

        return ContainerConfig.builder()
                .image(getDockerImageName())
                .exposedPorts("4444", "5900")
                .env("TZ=" + timeZone)
                .cmd("/opt/bin/entry_point.sh")
                .build();
    }

    @Override
    protected void customizeHostConfigBuilder(HostConfig.Builder hostConfigBuilder) {

        // For all containers we've been asked to link to, add a containername:alias link to the host config
        if (!this.containersToLink.isEmpty()) {
            List<String> links = new ArrayList<>();
            for (Map.Entry<String, LinkableContainer> entry : this.containersToLink.entrySet()) {
                links.add(entry.getValue().getContainerName() + ":" + entry.getKey());
            }
            hostConfigBuilder.links(links);
        }
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
    public int getPort() {
        return 5900;
    }

    @Override
    protected void waitUntilContainerStarted() {
        // Repeatedly try and open a webdriver session

        Retryables.retryUntilSuccess(30, TimeUnit.SECONDS, new Retryables.UnreliableSupplier<RemoteWebDriver>() {
            @Override
            public RemoteWebDriver get() throws Exception {
                RemoteWebDriver driver = new RemoteWebDriver(getSeleniumAddress(), desiredCapabilities);
                driver.getCurrentUrl();

                LOGGER.info("Obtained a connection to container ({})", BrowserWebDriverContainer.this.getSeleniumAddress());
                return driver;
            }
        });
    }
}
