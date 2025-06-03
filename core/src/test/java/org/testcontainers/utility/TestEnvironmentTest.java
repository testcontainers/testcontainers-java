package org.testcontainers.utility;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by rnorth on 03/07/2016.
 */
public class TestEnvironmentTest {

    @Test
    public void testCompareVersionGreaterThanSameMajor() {
        assertThat(new ComparableVersion("1.22").compareTo(new ComparableVersion("1.20")) > 0)
            .as("1.22 > 1.20")
            .isTrue();
    }

    @Test
    public void testCompareVersionEqual() {
        assertThat(new ComparableVersion("1.20"))
            .as("1.20 == 1.20")
            .isEqualByComparingTo(new ComparableVersion("1.20"));
    }

    @Test
    public void testCompareVersionGreaterThan() {
        assertThat(new ComparableVersion("2.10").compareTo(new ComparableVersion("1.20")) > 0)
            .as("2.10 > 1.20")
            .isTrue();
    }

    @Test
    public void testCompareVersionIgnoresExcessLength() {
        assertThat(new ComparableVersion("1.20"))
            .as("1.20 == 1.20.3")
            .isEqualByComparingTo(new ComparableVersion("1.20.3"));
    }
}
