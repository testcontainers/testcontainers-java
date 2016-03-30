package org.testcontainers.testng.runner;

import javaslang.control.Try;
import org.openqa.selenium.WebDriver;
import org.testcontainers.grid.enums.Browser;
import org.testcontainers.testng.selenium.SeleniumStandalone;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Test runner for standalone container. Extend it by test classes to automatically create standalone instance
 * with corresponding driver. Note that it uses -Dbrowser property for setting up required browser type.
 */
public class BaseStandaloneTest {

	private static SeleniumStandalone seleniumStandalone;
	private final Logger logger = Logger.getLogger(getClass().getName());

	@BeforeSuite
	public void prepareContainers() {
		Try.run(() -> seleniumStandalone = new SeleniumStandalone(Browser.getName(System.getProperty("browser", "firefox"), true)))
				.onFailure(ex -> logger.severe(ex.getMessage()));
	}

	@BeforeMethod
	public void setUp() {
		Try.run(() -> getGrid().ifPresent(SeleniumStandalone::createDriverAndStartRecording))
				.onFailure(ex -> logger.severe(ex.getMessage()));
	}

	@AfterMethod
	public void tearDown(final ITestResult result) {
		getGrid().ifPresent(grid -> grid.closeDriverAndStopRecording(result.getMethod().getMethodName(), result.isSuccess()));
	}

	@AfterSuite
	public void stopGrid() {
		getGrid().ifPresent(SeleniumStandalone::stopGrid);
	}

	public Optional<SeleniumStandalone> getGrid() {
		return Optional.ofNullable(seleniumStandalone);
	}

	public WebDriver getDriver() {
		return getGrid().map(SeleniumStandalone::getDriver)
				.orElseThrow(() -> new AssertionError("Can't find web driver"));
	}
}
