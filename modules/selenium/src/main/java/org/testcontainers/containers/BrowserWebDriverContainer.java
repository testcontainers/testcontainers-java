package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.ducttape.timeouts.Timeouts;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.traits.VncService;
import org.testcontainers.containers.wait.HostPortWaitStrategy;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.WaitAllStrategy;
import org.testcontainers.containers.wait.WaitStrategy;
import org.testcontainers.lifecycle.TestDescription;
import org.testcontainers.lifecycle.TestLifecycleAware;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * A chrome/firefox/custom container based on SeleniumHQ's standalone container sets.
 * <p>
 * The container should expose Selenium remote control protocol and VNC.
 */
public class BrowserWebDriverContainer<SELF extends BrowserWebDriverContainer<SELF>> extends GenericContainer<SELF> implements VncService, LinkableContainer, TestLifecycleAware {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:%s";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:%s";

    private static final String DEFAULT_PASSWORD = "secret";
    private static final int SELENIUM_PORT = 4444;
    private static final int VNC_PORT = 5900;

    @Nullable
    private DesiredCapabilities desiredCapabilities;
    private boolean customImageNameIsSet = false;

    @Nullable
    private RemoteWebDriver driver;
    private VncRecordingMode recordingMode = VncRecordingMode.RECORD_FAILING;
    private RecordingFileFactory recordingFileFactory;
    private File vncRecordingDirectory = new File("/tmp");

    private VncRecordingContainer vncRecordingContainer = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebDriverContainer.class);

    /**
     */
    public BrowserWebDriverContainer() {
        final WaitStrategy logWaitStrategy = new LogMessageWaitStrategy()
                .withRegEx(".*(RemoteWebDriver instances should connect to|Selenium Server is up and running).*\n")
                .withStartupTimeout(Duration.of(15, SECONDS));

        this.waitStrategy = new WaitAllStrategy()
                .withStrategy(logWaitStrategy)
                .withStrategy(new HostPortWaitStrategy())
                .withStartupTimeout(Duration.of(15, SECONDS));

        this.withRecordingFileFactory(new DefaultRecordingFileFactory());
    }

    /**
     * Constructor taking a specific webdriver container name and tag
     * @param dockerImageName
     */
    public BrowserWebDriverContainer(String dockerImageName) {
        this();
        super.setDockerImageName(dockerImageName);
        this.customImageNameIsSet = true;
        // We have to force SKIP mode for the recording by default because we don't know if the image has VNC or not
        recordingMode = VncRecordingMode.SKIP;
    }


    public SELF withDesiredCapabilities(DesiredCapabilities desiredCapabilities) {
        this.desiredCapabilities = desiredCapabilities;
        return self();
    }

    @NotNull
    @Override
    protected Set<Integer> getLivenessCheckPorts() {
        Integer seleniumPort = getMappedPort(SELENIUM_PORT);
        if (recordingMode == VncRecordingMode.SKIP) {
            return ImmutableSet.of(seleniumPort);
        } else {
            return ImmutableSet.of(seleniumPort, getMappedPort(VNC_PORT));
        }
    }

    @Override
    protected void configure() {

        if (desiredCapabilities == null) {
            throw new IllegalStateException();
        }

        if (recordingMode != VncRecordingMode.SKIP) {
            if (getNetwork() == null) {
                withNetwork(Network.SHARED);
            }

            vncRecordingContainer = new VncRecordingContainer(this)
                    .withVncPassword(DEFAULT_PASSWORD)
                    .withVncPort(VNC_PORT);
        }

        if (!customImageNameIsSet) {
            super.setDockerImageName(getImageForCapabilities(desiredCapabilities));
        }

        String timeZone = System.getProperty("user.timezone");

        if (timeZone == null || timeZone.isEmpty()) {
            timeZone = "Etc/UTC";
        }

        addExposedPorts(SELENIUM_PORT, VNC_PORT);
        addEnv("TZ", timeZone);
        addEnv("no_proxy", "localhost");
        setCommand("/opt/bin/entry_point.sh");

        /*
         * Some unreliability of the selenium browser containers has been observed, so allow multiple attempts to start.
         */
        setStartupAttempts(3);
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
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        driver = Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS,
                Timeouts.getWithTimeout(10, TimeUnit.SECONDS,
                        () ->
                                () -> new RemoteWebDriver(getSeleniumAddress(), desiredCapabilities)));

        if (vncRecordingContainer != null) {
            LOGGER.debug("Starting VNC recording");
            vncRecordingContainer.start();
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
    public void afterTest(TestDescription description, Optional<Throwable> throwable) {
        retainRecordingIfNeeded(description.getFilesystemFriendlyName(), !throwable.isPresent());
    }

    @Override
    public void stop() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception e) {
                LOGGER.debug("Failed to quit the driver", e);
            }
        }

        if (vncRecordingContainer != null) {
            try {
                vncRecordingContainer.stop();
            } catch (Exception e) {
                LOGGER.debug("Failed to stop vncRecordingContainer", e);
            }
        }

        super.stop();
    }

    private void retainRecordingIfNeeded(String prefix, boolean succeeded) {
        final boolean shouldRecord;
        switch (recordingMode) {
            case RECORD_ALL:
                shouldRecord = true;
                break;
            case RECORD_FAILING:
                shouldRecord = !succeeded;
                break;
            default:
                shouldRecord = false;
                break;
        }

        if (shouldRecord) {
            File recordingFile = recordingFileFactory.recordingFileForTest(vncRecordingDirectory, prefix, succeeded);
            LOGGER.info("Screen recordings for test {} will be stored at: {}", prefix, recordingFile);

            vncRecordingContainer.saveRecordingToFile(recordingFile);
        }
    }

    /**
     * Remember any other containers this needs to link to. We have to pass these down to the container so that
     * the other containers will be initialized before linking occurs.
     *
     * @param otherContainer the container rule to link to
     * @param alias          the alias (hostname) that this other container should be referred to by
     * @return this
     *
     * @deprecated Links are deprecated (see <a href="https://github.com/testcontainers/testcontainers-java/issues/465">#465</a>). Please use {@link Network} features instead.
     */
    @Deprecated
    public SELF withLinkToContainer(LinkableContainer otherContainer, String alias) {
        addLink(otherContainer, alias);
        return self();
    }

    public SELF withRecordingMode(VncRecordingMode recordingMode, File vncRecordingDirectory) {
        this.recordingMode = recordingMode;
        this.vncRecordingDirectory = vncRecordingDirectory;
        return self();
    }

    public SELF withRecordingFileFactory(RecordingFileFactory recordingFileFactory) {
        this.recordingFileFactory = recordingFileFactory;
        return self();
    }

    public enum VncRecordingMode {
        SKIP, RECORD_ALL, RECORD_FAILING
    }


}
