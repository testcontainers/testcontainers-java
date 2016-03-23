package org.testcontainers.grid.containers;

import javaslang.control.Try;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.VncRecordingSidekickContainer;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.traits.VncService;
import org.testcontainers.grid.enums.Browser;
import org.testcontainers.grid.enums.SeleniumImage;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class GenericGridContainer extends GenericContainer implements VncService, LinkableContainer {

	// Constants required for child containers
	public static final int DEFAULT_VNC_PORT = 5900;
	public static final int DEFAULT_HUB_PORT = 4444;
	public static final int DEFAULT_NODE_PORT = 5555;

	public static final String DEFAULT_HUB_URL = "http://localhost:4444/wd/hub";
	public static final String DEFAULT_HUB_API_URL = "http://localhost:4444/grid/api/hub";
	public static final String DEFAULT_PROXY_URL = "http://localhost:4444/grid/api/proxy?id=http://localhost:4444";
	public static final String DEFAULT_VNC_PASSWORD = "secret";

	public static final long POLLING_TIMEOUT = 60;
	public static final long POLLING_INTERVAL = 1;

	private VncRecordingMode recordingMode;
	private File vncRecordingDirectory;
	private VncRecordingSidekickContainer<GenericGridContainer> recordingContainer;
	private static final SimpleDateFormat FILENAME_DATE_FORMAT = new SimpleDateFormat("YYYYMMdd-HHmmss");
	private static final AtomicInteger RECORDINGS_COUNTER = new AtomicInteger();

	private Browser browser;
	private SeleniumImage image;

	public GenericGridContainer(final SeleniumImage image) {
		validateImage(image);
		super.setDockerImageName(image.toString());

		this.image = image;
		this.browser = image.getBrowser();
		this.recordingMode = VncRecordingMode.SKIP;
		this.vncRecordingDirectory = new File("/tmp");
	}

	public abstract void validateImage(SeleniumImage image);

	public abstract String getHubAddress();

	public abstract int getHubPort();

	@Override
	protected void configure() {
		final String timeZone = Optional.ofNullable(System.getProperty("user.timezone"))
				.filter(tZone -> tZone.length() > 0)
				.orElse("Etc/UTC");

		addEnv("TZ", timeZone);
		setCommand("/opt/bin/entry_point.sh");
	}

	@Override
	public String getVncAddress() {
		return "vnc://vnc:secret@" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_VNC_PORT);
	}

	@Override
	public String getPassword() {
		return DEFAULT_VNC_PASSWORD;
	}

	@Override
	public int getPort() {
		return DEFAULT_VNC_PORT;
	}

	public URL getSeleniumAddress() {
		return Try.of(() -> new URL("http", getHubAddress(), getHubPort(), "/wd/hub"))
				.getOrElseTry(() -> new URL(DEFAULT_HUB_URL));
	}

	public String getInternalIpAddress() {
		return Optional.ofNullable(getContainerInfo())
				.map(info -> info.getNetworkSettings().getIpAddress())
				.orElse("localhost");
	}

	public Browser getBrowser() {
		return browser;
	}

	public SeleniumImage getImage() {
		return image;
	}

	public void cleanUpRecordingsCounter() {
		RECORDINGS_COUNTER.set(0);
	}

	public VncRecordingMode getRecordingMode() {
		return recordingMode;
	}

	public void startRecording() {
		if (recordingMode != VncRecordingMode.SKIP) {
			logger().info("Starting VNC recording");

			if (recordingContainer != null && recordingContainer.isRunning()) {
				recordingContainer.stop();
			}

			recordingContainer = new VncRecordingSidekickContainer<>(this);
			recordingContainer.start();
		}
	}

	public void stopAndRetainRecording(final String testName, final boolean isPassed) {
		boolean finalizeRecords;

		switch (getRecordingMode()) {
			case RECORD_ALL:
				finalizeRecords = true;
				break;
			case RECORD_FAILING:
				finalizeRecords = isPassed;
				break;
			default:
			case SKIP:
				finalizeRecords = false;
				break;
		}

		if (finalizeRecords)
			stopAndRetainRecording(testName);
	}

	public void stopAndRetainRecording(final String testName) {
		final File recordingFile = new File(vncRecordingDirectory, "recording-" + testName + "-" +
				browser.toString() + "-" + FILENAME_DATE_FORMAT.format(new Date()) + "-" +
				RECORDINGS_COUNTER.incrementAndGet() + ".flv");

		Optional.ofNullable(recordingContainer)
				.ifPresent(recording -> {
					logger().info("Screen recordings for test {} will be stored at: {}", testName, recordingFile);
					recording.stopAndRetainRecording(recordingFile);
				});

		recordingContainer = null;
	}

	public void setRecordingMode(final VncRecordingMode recordingMode) {
		this.recordingMode = recordingMode;
	}

	public void setVncRecordingDirectory(final File vncRecordingDirectory) {
		if (!vncRecordingDirectory.isDirectory())
			throw new IllegalArgumentException(vncRecordingDirectory + " is not a directory");

		this.vncRecordingDirectory = vncRecordingDirectory;
	}

	public GenericGridContainer withLinkToContainer(final LinkableContainer otherContainer, final String alias) {
		addLink(otherContainer, alias);
		return this;
	}

	public GenericGridContainer withFileSystemBind(final String hostFolder, final String containerFolder, final BindMode permissions) {
		addFileSystemBind(hostFolder, containerFolder, permissions);
		return this;
	}

	public GenericGridContainer withRecordingMode(final VncRecordingMode recordingMode, final File vncRecordingDirectory) {
		setRecordingMode(recordingMode);
		setVncRecordingDirectory(vncRecordingDirectory);
		return this;
	}

	public enum VncRecordingMode {
		SKIP, RECORD_ALL, RECORD_FAILING
	}
}
