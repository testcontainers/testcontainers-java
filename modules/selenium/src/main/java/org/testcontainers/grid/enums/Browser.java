package org.testcontainers.grid.enums;

import java.util.stream.Stream;

public enum Browser {

	CHROME("chrome"),
	FIREFOX("firefox"),
	CHROME_STANDALONE("standalone-chrome"),
	FIREFOX_STANDALONE("standalone-firefox"),
	NONE("");

	private String name;

	Browser(final String name) {
		this.name = name;
	}

	public static Browser getName(final String browserName, final boolean isStandalone) {
		return Stream.of(values())
				.filter(browser -> browser.name.equals(isStandalone ? "standalone-" + browserName : browserName))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unable to get browser with name " + browserName));
	}

	public String toString() {
		return name;
	}
}
