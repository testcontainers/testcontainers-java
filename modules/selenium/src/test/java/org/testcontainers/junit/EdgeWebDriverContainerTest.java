package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.edge.EdgeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class EdgeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Container
    public BrowserWebDriverContainer<?> edge = new BrowserWebDriverContainer<>()
        .withCapabilities(new EdgeOptions())
        // }
        .withNetwork(NETWORK);

    @BeforeEach
    public void checkBrowserIsIndeedMSEdge() {
        assertBrowserNameIs(edge, "msedge", new EdgeOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(edge, new EdgeOptions());
    }
}
