package org.testcontainers.grid.enums;

import java.util.stream.Stream;

/**
 * A list of available SeleniumHQ images to give users more freedom in creating containers
 */
public enum SeleniumImage {

	HUB("selenium/hub:latest", Browser.NONE),
	FIREFOX_NODE_DEBUG("selenium/node-firefox-debug:2.52.0", Browser.FIREFOX),
	CHROME_NODE_DEBUG("selenium/node-chrome-debug:2.52.0", Browser.CHROME),
	CHROME_STANDALONE_DEBUG("selenium/standalone-chrome-debug:2.52.0", Browser.CHROME_STANDALONE),
	FIREFOX_STANDALONE_DEBUG("selenium/standalone-firefox-debug:2.52.0", Browser.FIREFOX_STANDALONE);

	private String name;
	private Browser browser;

	SeleniumImage(final String name, final Browser browser) {
		this.name = name;
		this.browser = browser;
	}

	public static SeleniumImage extractImage(final Browser browser) {
		return Stream.of(SeleniumImage.values())
				.filter(image -> browser.equals(image.browser))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unable to find image for " + browser.toString()));
	}

	public Browser getBrowser() {
		return browser;
	}

	@Override
	public String toString() {
		return name;
	}
}
