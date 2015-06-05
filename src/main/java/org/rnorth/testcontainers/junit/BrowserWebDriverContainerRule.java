package org.rnorth.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.testcontainers.containers.BrowserWebDriverContainer;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class BrowserWebDriverContainerRule extends ExternalResource {

    private final Collection<BrowserWebDriverContainer> containers = new ArrayList<>();
    private final Collection<RemoteWebDriver> drivers = new ArrayList<>();
    private final DesiredCapabilities desiredCapabilities;
    private final Map<RemoteWebDriver, String> vncUrls = new HashMap<>();
    private final Map<RemoteWebDriver, URL> seleniumUrls = new HashMap<>();

    public BrowserWebDriverContainerRule(DesiredCapabilities desiredCapabilities) {
        this.desiredCapabilities = desiredCapabilities;
    }

    @Override
    protected void after() {
        for (RemoteWebDriver driver : drivers) {
            driver.quit();
        }
        for (BrowserWebDriverContainer container : containers) {
            container.stop();
        }
    }

    /**
     * Obtain a new RemoteWebDriver instance that is bound to an instance of the browser running inside a new container.
     *
     * All containers and drivers will be automatically shut down after the test method finishes (if used as a @Rule) or the test
     * class (if used as a @ClassRule)
     *
     * @return a new Remote Web Driver instance
     */
    public RemoteWebDriver newDriver() {

        BrowserWebDriverContainer container = new BrowserWebDriverContainer(desiredCapabilities);
        containers.add(container);
        container.start();

        try {
            RemoteWebDriver driver = new RemoteWebDriver(container.getSeleniumAddress(), desiredCapabilities);
            drivers.add(driver);
            vncUrls.put(driver, container.getVncAddress());
            seleniumUrls.put(driver, container.getSeleniumAddress());
            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not determine webdriver URL", e);
        }
    }

    /**
     * Get the IP address that containers (browsers) can use to reference a service running on the local machine,
     * i.e. the machine on which this test is running.
     *
     * For example, if a web server is running on port 8080 on this local machine, the containerized web driver needs
     * to be pointed at "http://" + getHostIpAddress() + ":8080" in order to access it. Trying to hit localhost
     * from inside the container is not going to work, since the container has its own IP address.
     *
     * @return
     */
    public String getHostIpAddress() {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                // Running on a Mac therefore use boot2docker
                runShellCommand("/usr/local/bin/boot2docker", "up");
                String boot2dockerConfig = runShellCommand("/usr/local/bin/boot2docker", "config");

                for (String line : boot2dockerConfig.split("\n")) {
                    Matcher matcher = Pattern.compile("HostIP = \"(.+)\"").matcher(line);
                    if (matcher.matches()) {
                        return matcher.group(1);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        } else {
            throw new UnsupportedOperationException("This is only implemented for boot2docker right now");
        }
        throw new UnsupportedOperationException();
    }

    // TODO: Refactor into utility class
    private String runShellCommand(String... command) throws IOException, InterruptedException, TimeoutException {
        ProcessResult result;
        result = new ProcessExecutor().command(command)
                .readOutput(true).execute();

        if (result.getExitValue() != 0) {
            System.err.println(result.getOutput().getString());
            throw new IllegalStateException();
        }
        return result.outputUTF8().trim();
    }

    public String getVncUrl(RemoteWebDriver driver) {
        return vncUrls.get(driver);
    }

    public URL getSeleniumURL(RemoteWebDriver driver) {
        return seleniumUrls.get(driver);
    }
}