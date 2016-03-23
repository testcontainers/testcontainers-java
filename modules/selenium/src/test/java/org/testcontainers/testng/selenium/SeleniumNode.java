package org.testcontainers.testng.selenium;

import javaslang.control.Try;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.grid.enums.Browser;
import org.testcontainers.grid.containers.SeleniumNodeContainer;
import ru.stqa.selenium.factory.WebDriverFactory;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Simple wrapper for handling node container / webdriver instances.
 */
public class SeleniumNode {

	private Map.Entry<Browser, SeleniumNodeContainer> node;
	private WebDriver driver;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public SeleniumNode(final Map.Entry<Browser, SeleniumNodeContainer> node) {
		this.node = node;
	}

	public SeleniumNodeContainer getNode() {
		return node.getValue();
	}

	public Browser getBrowser() {
		return node.getKey();
	}

	public WebDriver getDriver() {
		return driver;
	}

	public void stopNode() {
		node.getValue().stop();
	}

	public SeleniumNode createDriverAndStartRecording() {
		this.driver = WebDriverFactory.getDriver(getNode().getSeleniumAddress().toExternalForm(),
				getCapabilities(getBrowser()));
		getNode().startVideoRecording();
		return this;
	}

	public void closeWebDriverAndStopRecording(final String testName, final boolean isPassed) {
		Optional.ofNullable(getDriver())
				.ifPresent(driver -> Try.run(driver::quit)
						.onFailure(ex -> logger.info("Cannot close WebDriver: " + ex.getMessage())));
		getNode().stopAndRetainRecording(testName, isPassed);
	}

	private Capabilities getCapabilities(final Browser browser) {
		Capabilities capabilities;

		switch (browser) {
			case CHROME:
			case CHROME_STANDALONE:
				capabilities = DesiredCapabilities.chrome();
				break;
			case FIREFOX:
			case FIREFOX_STANDALONE:
			default:
				capabilities = DesiredCapabilities.firefox();
				break;
		}

		return capabilities;
	}
}
