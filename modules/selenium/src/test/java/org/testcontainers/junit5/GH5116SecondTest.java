package org.testcontainers.junit5;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

class GH5116SecondTest extends BaseTest {

    @Test
    void testDriverIsRestarted() {
        assertThat(container.getWebDriver().getSessionId(), is(notNullValue()));
    }

    @Test
    void testDriverIsRestarted2() {
        assertThat(container.getWebDriver().getSessionId(), is(notNullValue()));
    }
}
