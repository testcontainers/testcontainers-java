package org.testcontainers.grid.containers;

import javaslang.control.Try;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHttpRequest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.grid.enums.SeleniumImage;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.jayway.awaitility.Awaitility.given;
import static org.testcontainers.grid.utils.SeleniumHttpUtils.checkHostState;

public class SeleniumHubContainer extends GenericGridContainer {

	public SeleniumHubContainer() {
		this(SeleniumImage.HUB);
		setRecordingMode(VncRecordingMode.SKIP);
	}

	public SeleniumHubContainer(final SeleniumImage image) {
		super(image);
	}

	@Override
	public void validateImage(final SeleniumImage image) {
		if (image != SeleniumImage.HUB)
			throw new IllegalArgumentException("Invalid image specified for hub container");
	}

	@Override
	protected void configure() {
		super.configure();
		addExposedPorts(DEFAULT_HUB_PORT);
	}

	@Override
	public SeleniumHubContainer withLinkToContainer(final LinkableContainer linkToContainer, final String alias) {
		super.withLinkToContainer(linkToContainer, alias);
		return this;
	}

	@Override
	public SeleniumHubContainer withFileSystemBind(final String hostFolder, final String containerFolder, final BindMode permissions) {
		super.withFileSystemBind(hostFolder, containerFolder, permissions);
		return this;
	}

	@Override
	public SeleniumHubContainer withRecordingMode(final VncRecordingMode recordingMode, final File vncRecordingDirectory) {
		super.withRecordingMode(recordingMode, vncRecordingDirectory);
		return this;
	}

	@Override
	protected void waitUntilContainerStarted() {
		given().ignoreExceptions()
				.atMost(POLLING_TIMEOUT, TimeUnit.SECONDS)
				.pollInterval(POLLING_INTERVAL, TimeUnit.SECONDS)
				.until(() -> {
					final boolean isConnected = checkHostState(new HttpHost(getHubAddress(), getHubPort()),
							new BasicHttpRequest("GET", getHubUrl().toExternalForm()));
					logger().info("Obtained a connection to ({}) = {}", getHubUrl(), isConnected);
					return isConnected;
				});
	}

	@Override
	protected Integer getLivenessCheckPort() {
		return getHubPort();
	}

	@Override
	public String getHubAddress() {
		return getContainerIpAddress();
	}

	@Override
	public int getHubPort() {
		return getMappedPort(DEFAULT_HUB_PORT);
	}

	public SeleniumHubContainer startHub() {
		super.start();
		return this;
	}

	private URL getHubUrl() {
		return Try.of(() -> new URL("http", getHubAddress(), getHubPort(), "/grid/api/hub"))
				.getOrElseTry(() -> new URL(DEFAULT_HUB_API_URL));
	}
}
