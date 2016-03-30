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

public class SeleniumNodeContainer extends GenericGridContainer {

	private String hubAddress;
	private int hubPort;

	public SeleniumNodeContainer() {
		this(SeleniumImage.CHROME_NODE_DEBUG);
		setRecordingMode(VncRecordingMode.RECORD_FAILING);
	}

	public SeleniumNodeContainer(final SeleniumImage image) {
		super(image);
	}

	@Override
	public void validateImage(final SeleniumImage image) {
		if (image != SeleniumImage.CHROME_NODE_DEBUG && image != SeleniumImage.FIREFOX_NODE_DEBUG)
			throw new IllegalArgumentException("Invalid image specified for node container");
	}

	public SeleniumNodeContainer withHubAddress(final String hubAddress) {
		this.hubAddress = hubAddress;
		return this;
	}

	public SeleniumNodeContainer withHubPort(final int hubPort) {
		this.hubPort = hubPort;
		return this;
	}

	@Override
	protected void waitUntilContainerStarted() {
		given().ignoreExceptions()
				.atMost(POLLING_TIMEOUT, TimeUnit.SECONDS)
				.pollInterval(POLLING_INTERVAL, TimeUnit.SECONDS)
				.until(() -> {
					final boolean isConnected = checkHostState(new HttpHost(getHubAddress(), getHubPort()),
							new BasicHttpRequest("GET", getProxyUrl().toExternalForm()));
					logger().info("Obtained a connection to ({}) = {}", getProxyUrl(), isConnected);
					return isConnected;
				});
	}

	@Override
	public SeleniumNodeContainer withRecordingMode(final VncRecordingMode recordingMode, final File vncRecordingDirectory) {
		super.withRecordingMode(recordingMode, vncRecordingDirectory);
		return this;
	}

	@Override
	public SeleniumNodeContainer withLinkToContainer(final LinkableContainer linkToContainer, final String alias) {
		super.withLinkToContainer(linkToContainer, alias);
		return this;
	}

	@Override
	public SeleniumNodeContainer withFileSystemBind(final String hostFolder, final String containerFolder, final BindMode permissions) {
		super.withFileSystemBind(hostFolder, containerFolder, permissions);
		return this;
	}

	@Override
	public String getHubAddress() {
		return hubAddress;
	}

	@Override
	public int getHubPort() {
		return hubPort;
	}

	@Override
	protected Integer getLivenessCheckPort() {
		return getMappedPort(DEFAULT_VNC_PORT);
	}

	@Override
	protected void configure() {
		super.configure();
		addExposedPorts(DEFAULT_VNC_PORT);
	}

	public SeleniumNodeContainer startNode() {
		super.start();
		cleanUpRecordingsCounter();
		return this;
	}

	public SeleniumNodeContainer startVideoRecording() {
		super.startRecording();
		return this;
	}

	private URL getProxyUrl() {
		return Try.of(() -> new URL("http", getHubAddress(), getHubPort(), "/grid/api/proxy?id=http://" +
				getInternalIpAddress() + ":" + DEFAULT_NODE_PORT)).getOrElseTry(() -> new URL(DEFAULT_PROXY_URL));
	}
}
