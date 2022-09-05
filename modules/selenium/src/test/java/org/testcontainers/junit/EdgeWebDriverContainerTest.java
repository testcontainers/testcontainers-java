package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.edge.EdgeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.Container;

public class EdgeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Container
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
