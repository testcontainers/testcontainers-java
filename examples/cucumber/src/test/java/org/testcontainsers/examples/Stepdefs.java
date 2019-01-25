package org.testcontainsers.examples;

import cucumber.api.Scenario;
import cucumber.api.java.After;
import cucumber.api.java.Before;
import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;
import cucumber.api.java.en.When;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.lifecycle.TestDescription;

import java.io.File;
import java.util.List;
import java.util.Optional;

import static junit.framework.TestCase.assertEquals;
import static org.testcontainers.containers.BrowserWebDriverContainer.VncRecordingMode.RECORD_ALL;

public class Stepdefs {

    BrowserWebDriverContainer container = new BrowserWebDriverContainer()
            .withCapabilities(new ChromeOptions())
            .withRecordingMode(RECORD_ALL, new File("target"));

    private String location;
    private String answer;

    @Before
    public void beforeScenario() {
        container.start();
    }

    @After
    public void afterScenario(Scenario scenario) {
        if (scenario.isFailed()) {
            container.afterTest(new TestDescription() {
                @Override
                public String getTestId() {
                    return scenario.getId();
                }

                @Override
                public String getFilesystemFriendlyName() {
                    return scenario.getName();
                }
            }, Optional.of(new RuntimeException()));
        }
        container.stop();
    }

    @Given("^location is \"([^\"]*)\"$")
    public void location_is(String location) throws Exception {
        this.location = location;
    }

    @When("^I ask is it possible to search here$")
    public void i_ask_is_it_possible_to_search_here() throws Exception {
        RemoteWebDriver driver = container.getWebDriver();
        driver.get(location);
        List<WebElement> searchInputs = driver.findElementsByTagName("input");
        answer = searchInputs != null && searchInputs.size() > 0 ? "YES" : "NOPE";
    }

    @Then("^I should be told \"([^\"]*)\"$")
    public void i_should_be_told(String expected) throws Exception {
        assertEquals(expected, answer);
    }


}
