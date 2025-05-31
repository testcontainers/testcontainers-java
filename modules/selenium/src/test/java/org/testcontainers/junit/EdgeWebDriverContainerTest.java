package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.edge.EdgeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.TestcontainersRule;

public class EdgeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Rule
    public TestcontainersRule<BrowserWebDriverContainer<?>> edge = new TestcontainersRule<>(
        new BrowserWebDriverContainer<>()
            .withCapabilities(new EdgeOptions())
            // }
            .withNetwork(NETWORK)
    );

    @Before
    public void checkBrowserIsIndeedMSEdge() {
        assertBrowserNameIs(edge.get(), "msedge", new EdgeOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(edge.get(), new EdgeOptions());
    }
}
