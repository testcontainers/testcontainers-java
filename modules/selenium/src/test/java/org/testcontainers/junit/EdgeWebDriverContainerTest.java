package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.edge.EdgeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

public class EdgeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Rule
    public BrowserWebDriverContainer<?> edge = new BrowserWebDriverContainer<>()
        .withCapabilities(new EdgeOptions())
        // }
        .withNetwork(NETWORK);

    @Before
    public void checkBrowserIsIndeedMSEdge() {
        assertBrowserNameIs(edge, "msedge", new EdgeOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(edge, new EdgeOptions());
    }
}
