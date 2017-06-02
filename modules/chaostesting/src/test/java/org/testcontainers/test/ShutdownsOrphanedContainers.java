package org.testcontainers.test;

import org.junit.After;
import org.testcontainers.utility.ResourceReaper;

abstract class ShutdownsOrphanedContainers {

    @After
    public void tearDown() throws Exception {
        ResourceReaper.instance().performCleanup();
    }
}
