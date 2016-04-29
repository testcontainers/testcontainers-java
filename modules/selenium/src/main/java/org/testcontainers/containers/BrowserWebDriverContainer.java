package org.testcontainers.containers;

import com.google.common.util.concurrent.Uninterruptibles;
import org.jetbrains.annotations.Nullable;
import org.junit.runner.Description;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.traits.VncService;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * A chrome/firefox/custom container based on SeleniumHQ's standalone container sets.
 * <p>
 * The container should expose Selenium remote control protocol and VNC.
 */
public class BrowserWebDriverContainer<SELF extends BrowserWebDriverContainer<SELF>> extends GenericContainer<SELF> implements VncService, LinkableContainer {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:%s";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:%s";

    private static final String DEFAULT_PASSWORD = "secret";
    private static final int SELENIUM_PORT = 4444;
    private static final int VNC_PORT = 5900;

    @Nullable
    private DesiredCapabilities desiredCapabilities;
    @Nullable
    private RemoteWebDriver driver;

    private VncRecordingMode recordingMode = VncRecordingMode.RECORD_FAILING;
    private File vncRecordingDirectory = new File("/tmp");
    private final Collection<VncRecordingSidekickContainer> currentVncRecordings = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebDriverContainer.class);

    private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");

    /**
     */
    public BrowserWebDriverContainer() {

    }


    public SELF withDesiredCapabilities(DesiredCapabilities desiredCapabilities) {
        super.setDockerImageName(getImageForCapabilities(desiredCapabilities));
        this.desiredCapabilities = desiredCapabilities;
        return self();
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(SELENIUM_PORT);
    }

    @Override
    protected void configure() {
        String timeZone = System.getProperty("user.timezone");

        if (timeZone == null || timeZone.isEmpty()) {
            timeZone = "Etc/UTC";
        }

        addExposedPorts(SELENIUM_PORT, VNC_PORT);
        addEnv("TZ", timeZone);
        setCommand("/opt/bin/entry_point.sh");
    }

    public static String getImageForCapabilities(DesiredCapabilities desiredCapabilities) {

        String seleniumVersion = SeleniumUtils.determineClasspathSeleniumVersion();

        String browserName = desiredCapabilities.getBrowserName();
        switch (browserName) {
            case BrowserType.CHROME:
                return String.format(CHROME_IMAGE, seleniumVersion);
            case BrowserType.FIREFOX:
                return String.format(FIREFOX_IMAGE, seleniumVersion);
            default:
                throw new UnsupportedOperationException("Browser name must be 'chrome' or 'firefox'; provided '" + browserName + "' is not supported");
        }
    }

    public URL getSeleniumAddress() {
        try {
            return new URL("http", getContainerIpAddress(), getMappedPort(SELENIUM_PORT), "/wd/hub");
        } catch (MalformedURLException e) {
            e.printStackTrace();// TODO
            return null;
        }
    }

    @Override
    public String getVncAddress() {
        return "vnc://vnc:secret@" + getContainerIpAddress() + ":" + getMappedPort(VNC_PORT);
    }

    @Override
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    @Override
    public int getPort() {
        return VNC_PORT;
    }

    @Override
    protected void waitUntilContainerStarted() {
        // Repeatedly try and open a webdriver session

        driver = Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
            RemoteWebDriver driver = new RemoteWebDriver(getSeleniumAddress(), desiredCapabilities);
            driver.getCurrentUrl();

            logger().info("Obtained a connection to container ({})", BrowserWebDriverContainer.this.getSeleniumAddress());
            return driver;
        });

        if (recordingMode != VncRecordingMode.SKIP) {
            LOGGER.debug("Starting VNC recording");
            VncRecordingSidekickContainer recordingSidekickContainer = new VncRecordingSidekickContainer<>(this);
            recordingSidekickContainer.start();
            currentVncRecordings.add(recordingSidekickContainer);
        }
    }

    /**
     * Obtain a RemoteWebDriver instance that is bound to an instance of the browser running inside a new container.
     * <p>
     * All containers and drivers will be automatically shut down after the test method finishes (if used as a @Rule) or the test
     * class (if used as a @ClassRule)
     *
     * @return a new Remote Web Driver instance
     */
    public RemoteWebDriver getWebDriver() {
        return driver;
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
        if (driver != null) {
            driver.quit();
        }
        this.stop();
    }

    private void stopAndRetainRecording(Description description) {
        File recordingFile = new File(vncRecordingDirectory, "recording-" + filenameDateFormat.format(new Date()) + ".flv");

        LOGGER.info("Screen recordings for test {} will be stored at: {}", description.getDisplayName(), recordingFile);

        for (VncRecordingSidekickContainer container : currentVncRecordings) {
            container.stopAndRetainRecording(recordingFile);
        }
    }

    /**
     * Remember any other containers this needs to link to. We have to pass these down to the container so that
     * the other containers will be initialized before linking occurs.
     *
     * @param otherContainer the container rule to link to
     * @param alias          the alias (hostname) that this other container should be referred to by
     * @return this
     */
    public SELF withLinkToContainer(LinkableContainer otherContainer, String alias) {
        addLink(otherContainer, alias);
        return self();
    }

    public SELF withRecordingMode(VncRecordingMode recordingMode, File vncRecordingDirectory) {
        this.recordingMode = recordingMode;
        this.vncRecordingDirectory = vncRecordingDirectory;
        return self();
    }


    public enum VncRecordingMode {
        SKIP, RECORD_ALL, RECORD_FAILING;
    }


}
