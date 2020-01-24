package org.testcontainers.utility;

import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Created by rnorth on 03/07/2016.
 */
public class TestEnvironmentTest {

    @Test
    public void testCompareVersionGreaterThanSameMajor() {
        assertTrue("1.22 > 1.20", new ComparableVersion("1.22").compareTo(new ComparableVersion("1.20")) > 0);
    }

    @Test
    public void testCompareVersionEqual() {
        assertTrue("1.20 == 1.20", new ComparableVersion("1.20").compareTo(new ComparableVersion("1.20")) == 0);
    }

    @Test
    public void testCompareVersionGreaterThan() {
        assertTrue("2.10 > 1.20", new ComparableVersion("2.10").compareTo(new ComparableVersion("1.20")) > 0);
    }

    @Test
    public void testCompareVersionIgnoresExcessLength() {
        assertTrue("1.20 == 1.20.3", new ComparableVersion("1.20").compareTo(new ComparableVersion("1.20.3")) == 0);
    }
}
