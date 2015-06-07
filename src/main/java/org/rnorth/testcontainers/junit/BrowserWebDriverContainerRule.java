package org.rnorth.testcontainers.junit;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.rnorth.testcontainers.containers.BrowserWebDriverContainer;
import org.rnorth.testcontainers.utility.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.StartedProcess;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static org.rnorth.testcontainers.utility.CommandLine.executableExists;
import static org.rnorth.testcontainers.utility.CommandLine.runShellCommand;

/**
 *
 */
public class BrowserWebDriverContainerRule extends TestWatcher {

    private final Collection<BrowserWebDriverContainer> containers = new ArrayList<>();
    private final Collection<RemoteWebDriver> drivers = new ArrayList<>();
    private final DesiredCapabilities desiredCapabilities;
    private final Map<RemoteWebDriver, String> vncUrls = new HashMap<>();
    private final Map<RemoteWebDriver, URL> seleniumUrls = new HashMap<>();

    private final VncRecordingMode recordingMode;
    private final File vncRecordingDirectory;
    private Collection<VncRecordingUtilityWrapper> currentVncRecordings = new ArrayList<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserWebDriverContainerRule.class);

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
                File recordingFile = File.createTempFile("recording", System.currentTimeMillis() + ".flv", vncRecordingDirectory);
                VncRecordingUtilityWrapper recording = new VncRecordingUtilityWrapper(container.getVncAddress(), recordingFile);
                recording.start();
                currentVncRecordings.add(recording);
            }

            return driver;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not determine webdriver URL", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not create file for VNC recording in " + vncRecordingDirectory);
        }
    }

    @Override
    protected void finished(Description description) {
        for (RemoteWebDriver driver : drivers) {
            driver.quit();
        }
        for (BrowserWebDriverContainer container : containers) {
            container.stop();
        }
    }

    @Override
    protected void failed(Throwable e, Description description) {
        if (recordingMode != VncRecordingMode.SKIP) {
            LOGGER.info("Screen recordings for failing test {} have been captured at: {}", description.getDisplayName(), currentVncRecordings);
        }
        currentVncRecordings.stream().forEach(it -> it.stop(false));
    }

    @Override
    protected void succeeded(Description description) {
        if (recordingMode == VncRecordingMode.RECORD_FAILING) {
            // make sure all recordings relating to this test are cleaned up when the JVM exits
            currentVncRecordings.stream().forEach(it -> it.stop(true));
        }

        currentVncRecordings.clear();
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

    public static class VncRecordingUtilityWrapper {

        private static final Logger LOGGER = LoggerFactory.getLogger(VncRecordingUtilityWrapper.class);
        private static final Pattern VNC_URL_PATTERN = Pattern.compile("vnc://[^:]+:(?<password>[^@]+)@(?<host>[^:]+):(?<port>\\d+)");
        private final String vncURL;
        private final File outputFilename;
        private StartedProcess process;

        public VncRecordingUtilityWrapper(String vncURL, File outputFilename) {
            this.vncURL = vncURL;
            this.outputFilename = outputFilename;
        }

        public void start() {
            Matcher matcher = VNC_URL_PATTERN.matcher(vncURL);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("VNC URL could not be parsed! " + vncURL);
            }

            if (!CommandLine.executableExists("flvrec.py")) {
                LOGGER.info("flvrec.py is not available; VNC session will not be recorded");
                return;
            }

            try {
                Path passwordFile = Files.createTempFile("vncpassword", "");
                Files.write(passwordFile, matcher.group("password").getBytes(), StandardOpenOption.APPEND);
                process = CommandLine.runShellCommandInBackground("flvrec.py",
                        "-o", outputFilename.getCanonicalPath(),
                        "-P", passwordFile.toString(),
                        matcher.group("host"),
                        matcher.group("port"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stop(boolean discard) {
            if (process == null) {
                return;
            }

            process.getProcess().destroy();

            if (discard) {
                outputFilename.delete();
            }
        }
    }

    public enum VncRecordingMode {
        SKIP, RECORD_ALL, RECORD_FAILING
    }
}