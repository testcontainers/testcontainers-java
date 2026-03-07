package org.testcontainers.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.edge.EdgeOptions;

class EdgeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    public BrowserWebDriverContainer edge = new BrowserWebDriverContainer("selenium/standalone-edge:4.13.0")
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
