package org.testcontainers.testng.tests;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.testng.runner.BaseTest;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SeleniumGridParallelTests extends BaseTest {

	@Test
	public void simplePlainSeleniumTest() {
		WebDriver driver = getDriver();
		assertNotNull(driver);

		driver.get("https://wikipedia.org");
		WebElement searchInput = driver.findElement(By.name("search"));

		searchInput.sendKeys("Rick Astley");
		searchInput.submit();

		WebElement otherPage = driver.findElement(By.linkText("Rickrolling"));
		otherPage.click();

		boolean expectedTextFound = driver.findElements(By.cssSelector("p"))
				.stream()
				.anyMatch(element -> element.getText().contains("meme"));

		assertTrue(expectedTextFound, "The word 'meme' is found on a page about rickrolling");
	}

	@Test
	public void googleSearchTest() {
		WebDriver driver = getDriver();
		assertNotNull(driver);

		driver.get("https://google.com.ua");
		driver.findElement(By.id("lst-ib")).sendKeys("automation" + Keys.ENTER);

		new WebDriverWait(driver, 10)
				.until((WebDriver d) -> d.findElements(By.cssSelector(".r>a")).size() > 0);

		assertEquals("automation", driver.findElement(By.id("lst-ib")).getAttribute("value"));
	}
}
