package org.rnorth.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class BrowserWebDriverContainer extends AbstractContainer {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:2.45.0";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:2.45.0";

    private Map<String, List<PortBinding>> ports;
    private DesiredCapabilities desiredCapabilities;
    private String imageName = null;
    private String seleniumPort;
    private String vncPort;

    public BrowserWebDriverContainer(String imageName) {
        this.imageName = imageName;
    }

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
        return ContainerConfig.builder()
                .image(getDockerImageName())
                .exposedPorts("4444", "5900")
                .cmd("/opt/bin/entry_point.sh")
                .build();
    }

    @Override
    protected String getDockerImageName() {

        if (imageName != null) {
            return imageName;
        }

        String browserName = desiredCapabilities.getBrowserName();
        if (browserName.equals(BrowserType.CHROME)) {
            return CHROME_IMAGE;
        } else if (browserName.equals(BrowserType.FIREFOX)) {
            return FIREFOX_IMAGE;
        } else {
            throw new UnsupportedOperationException("Browser name must be 'chrome' or 'firefox'; provided '" + browserName + "' is not supported");
        }
    }

    public URL getSeleniumAddress() throws MalformedURLException {
        return new URL("http", dockerHostIpAddress, Integer.valueOf(this.seleniumPort), "/wd/hub");
    }

    public String getVncAddress() throws MalformedURLException {
        return "vnc://vnc:secret@" + dockerHostIpAddress + ":" + this.vncPort;
    }
}
