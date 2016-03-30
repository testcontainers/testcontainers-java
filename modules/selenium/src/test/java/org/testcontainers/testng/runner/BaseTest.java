package org.testcontainers.testng.runner;

import javaslang.control.Try;
import org.openqa.selenium.WebDriver;
import org.testcontainers.grid.enums.Browser;
import org.testcontainers.grid.containers.SeleniumHubContainer;
import org.testcontainers.grid.containers.SeleniumNodeContainer;
import org.testcontainers.testng.config.XMLConfig;
import org.testcontainers.testng.pool.PoolFactory;
import org.testcontainers.testng.selenium.SeleniumNode;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.vibur.objectpool.PoolService;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Test runner for separate hubContainer / nodes containers with pooling feature. Extend it by test classes to automatically
 * create standalone instance with corresponding driver. Note that it uses 'browser' parameter, picked from testng xml,
 * for setting up required browser types. Use -DscaleChrome / -DscaleFirefox properties to allocate required amount
 * of node containers.
 */
public class BaseTest {

	private static final int FIREFOX_POOL_SIZE = Integer.valueOf(System.getProperty("scaleFirefox", "1"));
	private static final int CHROME_POOL_SIZE = Integer.valueOf(System.getProperty("scaleChrome", "1"));

	private static SeleniumHubContainer hubContainer;
	private static PoolService<SeleniumNodeContainer> firefoxPool;
	private static PoolService<SeleniumNodeContainer> chromePool;

	private SeleniumNode seleniumNode;

	@BeforeSuite
	public void startGrid() {
		Try.run(() -> {
			hubContainer = new SeleniumHubContainer().startHub();
			firefoxPool = PoolFactory.getNodePool(hubContainer, Browser.FIREFOX, FIREFOX_POOL_SIZE);
			chromePool = PoolFactory.getNodePool(hubContainer, Browser.CHROME, CHROME_POOL_SIZE);
		}).getOrElseThrow((Function<Throwable, AssertionError>) AssertionError::new);
	}

	@BeforeMethod
	public void setUp(final ITestContext context) {
		Try.run(() -> {
			final XMLConfig config = new XMLConfig(context.getCurrentXmlTest().getAllParameters());
			seleniumNode = new SeleniumNode(takeNode(config.getBrowser()))
					.createDriverAndStartRecording();
		}).getOrElseThrow((Function<Throwable, AssertionError>) AssertionError::new);
	}

	@AfterMethod
	public void tearDown(final ITestResult result) {
		getNode().ifPresent(node ->
				node.closeWebDriverAndStopRecording(result.getMethod().getMethodName(), result.isSuccess()));
		restoreNode();
	}

	@AfterSuite
	public void stopGrid() {
		terminatePools();
		terminateHub();
	}

	public WebDriver getDriver() {
		return getNode().map(SeleniumNode::getDriver)
				.orElseThrow(() -> new AssertionError("Can't find web driver"));
	}

	private Optional<SeleniumNode> getNode() {
		return Optional.ofNullable(seleniumNode);
	}

	private void terminatePools() {
		getFirefoxPool().ifPresent(PoolService::terminate);
		getChromePool().ifPresent(PoolService::terminate);
	}

	private void terminateHub() {
		hubContainer.stop();
	}

	private Map.Entry<Browser, SeleniumNodeContainer> takeNode(final Browser browser) {
		SeleniumNodeContainer currentNode;
		switch (browser) {
			case CHROME:
				currentNode = getChromePool().map(PoolService::take)
						.map(SeleniumNodeContainer::startNode)
						.map(SeleniumNodeContainer::startVideoRecording)
						.orElseThrow(() -> new IllegalArgumentException("Unable to take chrome node"));
				break;
			default:
			case FIREFOX:
				currentNode = getFirefoxPool().map(PoolService::take)
						.map(SeleniumNodeContainer::startNode)
						.map(SeleniumNodeContainer::startVideoRecording)
						.orElseThrow(() -> new IllegalArgumentException("Unable to take firefox node"));
				break;
		}

		return new AbstractMap.SimpleImmutableEntry<>(browser, currentNode);
	}

	private void restoreNode() {
		getNode().ifPresent(gridContainer -> {
			switch (gridContainer.getBrowser()) {
				case CHROME:
					getChromePool().ifPresent(pool -> pool.restore(gridContainer.getNode()));
					break;
				default:
				case FIREFOX:
					getFirefoxPool().ifPresent(pool -> pool.restore(gridContainer.getNode()));
					break;
			}
			gridContainer.stopNode();
		});
	}

	private Optional<PoolService<SeleniumNodeContainer>> getFirefoxPool() {
		return Optional.ofNullable(firefoxPool);
	}

	private Optional<PoolService<SeleniumNodeContainer>> getChromePool() {
		return Optional.ofNullable(chromePool);
	}
}
