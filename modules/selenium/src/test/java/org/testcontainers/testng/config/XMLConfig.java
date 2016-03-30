package org.testcontainers.testng.config;

import org.testcontainers.grid.enums.Browser;

import java.util.Map;

/**
 * Special container for storing TestNG xml parameters.
 */
public class XMLConfig {

	private Map<String, String> parameters;

	public XMLConfig(final Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public String getParameter(final String key) {
		return parameters.getOrDefault(key, "");
	}

	public Browser getBrowser() {
		return getBrowser(false);
	}

	public Browser getBrowser(final boolean isStandalone) {
		return Browser.getName(getParameter("browser"), isStandalone);
	}
}
