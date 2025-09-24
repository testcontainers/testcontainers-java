package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.edge.EdgeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

class EdgeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    public BrowserWebDriverContainer<?> edge = new BrowserWebDriverContainer<>()
        .withCapabilities(new EdgeOptions())
        // }
        .withNetwork(NETWORK);

    @BeforeEach
    public void checkBrowserIsIndeedMSEdge() {
        edge.start();
        assertBrowserNameIs(edge, "msedge", new EdgeOptions());
    }

    @Test
    void simpleExploreTest() {
        doSimpleExplore(edge, new EdgeOptions());
    }
}
