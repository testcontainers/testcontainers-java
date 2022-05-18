package org.testcontainers.junit5;


import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class GH5116FirstTest extends BaseTest {

    @Test
    void testDriverIsRestarted3() {
        assertThat(container.getWebDriver().getSessionId(), is(notNullValue()));
        assertThat(container.getVncAddress(), is(notNullValue()));
    }

    @Test
    void testDriverIsRestarted4() {
        assertThat(container.getWebDriver().getSessionId(), is(notNullValue()));
        assertThat(container.getVncAddress(), is(notNullValue()));
    }

}
