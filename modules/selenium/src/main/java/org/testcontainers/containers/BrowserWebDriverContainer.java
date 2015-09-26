package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.PortBinding;
import org.junit.runner.Description;
import org.openqa.selenium.remote.BrowserType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.traits.VncService;
import org.testcontainers.junit.BrowserWebDriverContainerRule;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static org.testcontainers.utility.CommandLine.executableExists;
import static org.testcontainers.utility.CommandLine.runShellCommand;

/**
 * A chrome/firefox/custom container based on SeleniumHQ's standalone container sets.
 *
 * The container should expose Selenium remote control protocol and VNC.
 */
public class BrowserWebDriverContainer extends GenericContainer implements VncService, LinkableContainer {

    private static final String CHROME_IMAGE = "selenium/standalone-chrome-debug:2.45.0";
    private static final String FIREFOX_IMAGE = "selenium/standalone-firefox-debug:2.45.0";
    private static final String DEFAULT_PASSWORD = "secret";

    private DesiredCapabilities desiredCapabilities;
    private Map<String, LinkableContainer> containersToLink = new HashMap<>();
    private String seleniumPort;
    private String vncPort;
    private RemoteWebDriver driver;

    private VncRecordingMode recordingMode = VncRecordingMode.RECORD_FAILING;
    private File vncRecordingDirectory = new File("/tmp");
    private Collection<VncRecordingSidekickContainer> currentVncRecordings = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebDriverContainerRule.class);

    private static final SimpleDateFormat filenameDateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");

    /**
     */
    public BrowserWebDriverContainer() {

    }

    public BrowserWebDriverContainer withDesiredCapabilities(DesiredCapabilities desiredCapabilities) {
        this.dockerImageName = getImageForCapabilities(desiredCapabilities);
        this.desiredCapabilities = desiredCapabilities;
        return this;
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

//        HostConfig.Builder hostConfigBuilder = HostConfig.builder();
//        // For all containers we've been asked to link to, add a containername:alias link to the host config
//        if (!this.containersToLink.isEmpty()) {
//            List<String> links = new ArrayList<>();
//            for (Map.Entry<String, LinkableContainer> entry : this.containersToLink.entrySet()) {
//                links.add(entry.getValue().getContainerName() + ":" + entry.getKey());
//            }
//            hostConfigBuilder.links(links);
//        }

        withImageName(getDockerImageName());
        withExposedPorts(4444, 5900);
        withEnv("TZ", timeZone);
        withCommand("/opt/bin/entry_point.sh");
        for (Map.Entry<String, LinkableContainer> other : containersToLink.entrySet()) {
            withLinkToContainer(other.getValue(), other.getKey());
        }

        return super.getContainerConfig();
//
//        return ContainerConfig.builder()
//                .image(getDockerImageName())
//                .exposedPorts("4444", "5900")
//                .env("TZ=" + timeZone)
//                .cmd("/opt/bin/entry_point.sh")
//                .hostConfig(hostConfigBuilder.build())
//                .build();
    }

    public static String getImageForCapabilities(DesiredCapabilities desiredCapabilities) {

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

    public URL getSeleniumAddress() {
        try {
            return new URL("http", getIpAddress(), Integer.valueOf(this.seleniumPort), "/wd/hub");
        } catch (MalformedURLException e) {
            e.printStackTrace();// TODO
            return null;
        }
    }

    @Override
    public String getVncAddress() {
        return "vnc://vnc:secret@" + getIpAddress() + ":" + this.vncPort;
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

        driver = Unreliables.retryUntilSuccess(30, TimeUnit.SECONDS, () -> {
            RemoteWebDriver driver = new RemoteWebDriver(getSeleniumAddress(), desiredCapabilities);
            driver.getCurrentUrl();

            logger().info("Obtained a connection to container ({})", BrowserWebDriverContainer.this.getSeleniumAddress());
            return driver;
        });
    }

    public RemoteWebDriver getDriver() {
        return driver;
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

//        final Map<String, LinkableContainer> containersToLink = new HashMap<>();
//        for (Map.Entry<String, LinkableContainerRule> entry : containerRulesToLink.entrySet()) {
//            containersToLink.put(entry.getKey(), entry.getValue().getContainer());
//        }

//        BrowserWebDriverContainer container = Retryables.retryUntilSuccess(30, TimeUnit.SECONDS, new Retryables.UnreliableSupplier<BrowserWebDriverContainer>() {
//            @Override
//            public BrowserWebDriverContainer get() throws Exception {
//                Future<BrowserWebDriverContainer> future = Executors.newSingleThreadExecutor().submit(new Callable<BrowserWebDriverContainer>() {
//                    @Override
//                    public BrowserWebDriverContainer call() throws Exception {
//                        BrowserWebDriverContainer container = new BrowserWebDriverContainer(getImageForCapabilities(desiredCapabilities), desiredCapabilities, containersToLink);
//                        container.start();
//
//                        return container;
//                    }
//                });
//                return future.get(10, TimeUnit.SECONDS);
//            }
//        });
//        RemoteWebDriver driver = container.getDriver();

        RemoteWebDriver driver = this.getDriver();

        if (recordingMode != VncRecordingMode.SKIP) {
            LOGGER.debug("Starting VNC recording");
            VncRecordingSidekickContainer<BrowserWebDriverContainer> recordingSidekickContainer = new VncRecordingSidekickContainer<>(this);
            recordingSidekickContainer.start();
            currentVncRecordings.add(recordingSidekickContainer);
        }

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
        driver.quit();
        this.stop();
    }

    private void stopAndRetainRecording(Description description) {
        File recordingFile = new File(vncRecordingDirectory, "recording-" + filenameDateFormat.format(new Date()) + ".flv");

        LOGGER.info("Screen recordings for test {} will be stored at: {}", description.getDisplayName(), recordingFile);

        for(VncRecordingSidekickContainer container : currentVncRecordings) {
            container.stopAndRetainRecording(recordingFile);
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

    /**
     * Remember any other containers this needs to link to. We have to pass these down to the container so that
     * the other containers will be initialized before linking occurs.
     *
     * @param containerRule the container rule to link to
     * @param alias the alias (hostname) that this other container should be referred to by
     * @return
     */
    public BrowserWebDriverContainer withLinkToContainer(LinkableContainer containerRule, String alias) {
        this.containersToLink.put(alias, containerRule);
        return this;
    }

    public BrowserWebDriverContainer withRecordingMode(VncRecordingMode recordingMode, File vncRecordingDirectory) {
        this.recordingMode = recordingMode;
        this.vncRecordingDirectory = vncRecordingDirectory;
        return this;
    }

    public enum VncRecordingMode {
        SKIP, RECORD_ALL, RECORD_FAILING
    }
}
