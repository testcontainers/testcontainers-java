package org.testcontainers.testng.selenium;

import javaslang.control.Try;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.grid.enums.Browser;
import org.testcontainers.grid.containers.GenericGridContainer;
import org.testcontainers.grid.containers.SeleniumStandaloneContainer;
import ru.stqa.selenium.factory.WebDriverFactory;

import java.io.File;
import java.util.Optional;
import java.util.logging.Logger;

import static org.testcontainers.grid.enums.SeleniumImage.extractImage;

/**
 * Simple wrapper for handling standalone container / webdriver instances.
 */
public class SeleniumStandalone {

	private SeleniumStandaloneContainer standaloneContainer;
	private WebDriver driver;

	private final Logger logger = Logger.getLogger(getClass().getName());

	public SeleniumStandalone(final Browser browser) {
		this.standaloneContainer = new SeleniumStandaloneContainer(extractImage(browser))
				.withRecordingMode(GenericGridContainer.VncRecordingMode.RECORD_ALL, new File("target"))
				.startGrid();
	}

	public SeleniumStandaloneContainer getStandalone() {
		return standaloneContainer;
	}

	public void stopGrid() {
		standaloneContainer.stop();
	}

	public void closeDriverAndStopRecording(final String testName, final boolean isPassed) {
		Optional.ofNullable(getDriver())
				.ifPresent(driver -> Try.run(driver::quit)
						.onFailure(ex -> logger.info("Can't close WebDriver: " + ex.getMessage())));
		getStandalone().stopAndRetainRecording(testName, isPassed);
	}

	public void createDriverAndStartRecording() {
		this.driver = WebDriverFactory.getDriver(getStandalone().getSeleniumAddress().toExternalForm(),
				getCapabilities(getStandalone().getBrowser()));
		getStandalone().startRecording();
	}

	public WebDriver getDriver() {
		return driver;
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
