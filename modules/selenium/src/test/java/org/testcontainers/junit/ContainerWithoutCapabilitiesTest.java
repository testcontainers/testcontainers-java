package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.BrowserWebDriverContainer;

public class ContainerWithoutCapabilitiesTest extends BaseWebDriverContainerTest{

    @Rule
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
        .withNetwork(NETWORK);

    @Test
    public void chromeIsStartedIfNoCapabilitiesProvided() {
        assertBrowserNameIs(chrome, "chrome");
    }

    @Test
    public void simpleExploreTestWhenNoCapabilitiesProvided(){
        doSimpleExplore(chrome);
    }
}
