package org.testcontainers.grid.containers;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.grid.enums.SeleniumImage;

import java.io.File;

public class SeleniumStandaloneContainer extends GenericGridContainer {

	public SeleniumStandaloneContainer() {
		this(SeleniumImage.CHROME_STANDALONE_DEBUG);
		setRecordingMode(VncRecordingMode.RECORD_FAILING);
	}

	public SeleniumStandaloneContainer(final SeleniumImage image) {
		super(image);
	}

	@Override
	public void validateImage(final SeleniumImage image) {
		if (image != SeleniumImage.CHROME_STANDALONE_DEBUG && image != SeleniumImage.FIREFOX_STANDALONE_DEBUG)
			throw new IllegalArgumentException("Invalid image specified for standalone container");
	}

	@Override
	public SeleniumStandaloneContainer withRecordingMode(final VncRecordingMode recordingMode, final File vncRecordingDirectory) {
		super.withRecordingMode(recordingMode, vncRecordingDirectory);
		return this;
	}

	@Override
	public SeleniumStandaloneContainer withLinkToContainer(final LinkableContainer linkToContainer, final String alias) {
		super.withLinkToContainer(linkToContainer, alias);
		return this;
	}

	@Override
	public SeleniumStandaloneContainer withFileSystemBind(final String hostFolder, final String containerFolder, final BindMode permissions) {
		super.withFileSystemBind(hostFolder, containerFolder, permissions);
		return this;
	}

	@Override
	public String getHubAddress() {
		return getContainerIpAddress();
	}

	@Override
	public int getHubPort() {
		return getMappedPort(DEFAULT_HUB_PORT);
	}

	@Override
	protected Integer getLivenessCheckPort() {
		return getMappedPort(DEFAULT_HUB_PORT);
	}

	@Override
	protected void configure() {
		super.configure();
		addExposedPorts(DEFAULT_HUB_PORT, DEFAULT_VNC_PORT);
	}

	public SeleniumStandaloneContainer startGrid() {
		super.start();
		return this;
	}
}
