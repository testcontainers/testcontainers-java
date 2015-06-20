package org.rnorth.testcontainers.junit;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.testcontainers.containers.AbstractContainer;
import org.rnorth.testcontainers.containers.BrowserWebDriverContainer;
import org.rnorth.testcontainers.containers.VncRecordingSidekickContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static org.rnorth.testcontainers.utility.CommandLine.executableExists;
import static org.rnorth.testcontainers.utility.CommandLine.runShellCommand;

/**
 *
 */
public class BrowserWebDriverContainerRule extends TestWatcher {

    private final Collection<AbstractContainer> containers = new ArrayList<>();
    private final Collection<RemoteWebDriver> drivers = new ArrayList<>();
    private final DesiredCapabilities desiredCapabilities;
    private final Map<RemoteWebDriver, String> vncUrls = new HashMap<>();
    private final Map<RemoteWebDriver, URL> seleniumUrls = new HashMap<>();

    private final VncRecordingMode recordingMode;
    private final File vncRecordingDirectory;
    private Collection<VncRecordingSidekickContainer> currentVncRecordings = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebDriverContainerRule.class);

    private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");

    public BrowserWebDriverContainerRule(DesiredCapabilities desiredCapabilities) {
        this(desiredCapabilities, VncRecordingMode.SKIP, null);
    }

    public BrowserWebDriverContainerRule(DesiredCapabilities desiredCapabilities, VncRecordingMode recordingMode, File vncRecordingDirectory) {
        this.desiredCapabilities = desiredCapabilities;
        this.recordingMode = recordingMode;
        this.vncRecordingDirectory = vncRecordingDirectory;
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

            if (recordingMode != VncRecordingMode.SKIP) {
                LOGGER.debug("Starting VNC recording");
                VncRecordingSidekickContainer<BrowserWebDriverContainer> recordingSidekickContainer = new VncRecordingSidekickContainer<>(container);
                recordingSidekickContainer.start();
                currentVncRecordings.add(recordingSidekickContainer);
            }

            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not determine webdriver URL", e);
        }
    }

    @Override
    protected void failed(Throwable e, Description description) {

        switch (recordingMode) {
            case RECORD_FAILING:
            case RECORD_ALL:
                stopAndRetainRecording(description);
                break;
        }
        currentVncRecordings.clear();
    }

    @Override
    protected void succeeded(Description description) {

        switch (recordingMode) {
            case RECORD_ALL:
                stopAndRetainRecording(description);
                break;
        }
        currentVncRecordings.clear();
    }

    @Override
    protected void finished(Description description) {
        for (RemoteWebDriver driver : drivers) {
            driver.quit();
        }
        for (AbstractContainer container : containers) {
            container.stop();
        }
    }

    private void stopAndRetainRecording(Description description) {
        File recordingFile = new File(vncRecordingDirectory, "recording-" + filenameDateFormat.format(new Date()) + ".flv");

        LOGGER.info("Screen recordings for test {} will be stored at: {}", description.getDisplayName(), recordingFile);
        currentVncRecordings.stream().forEach(it -> it.stopAndRetainRecording(recordingFile));
    }

    /**
     * Get the IP address that containers (browsers) can use to reference a service running on the local machine,
     * i.e. the machine on which this test is running.
     *
     * For example, if a web server is running on port 8080 on this local machine, the containerized web driver needs
     * to be pointed at "http://" + getHostIpAddress() + ":8080" in order to access it. Trying to hit localhost
     * from inside the container is not going to work, since the container has its own IP address.
     *
     * @return the IP address of the host machine
     */
    public String getHostIpAddress() {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                // Running on a Mac therefore use boot2docker
                checkArgument(executableExists("/usr/local/bin/boot2docker"), "boot2docker must be installed for use on OS X");
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

    public String getVncUrl(RemoteWebDriver driver) {
        return vncUrls.get(driver);
    }

    public URL getSeleniumURL(RemoteWebDriver driver) {
        return seleniumUrls.get(driver);
    }

    public enum VncRecordingMode {
        SKIP, RECORD_ALL, RECORD_FAILING
    }
}