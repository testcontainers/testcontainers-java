package org.testcontainers.junit;

import com.google.common.base.Stopwatch;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.RemoteDockerImage;

import java.util.concurrent.TimeUnit;

/**
 * Created by rnorth on 14/04/2017.
 */
public class PullCachePerformanceTest {

    @Test
    public void testWithoutNameFilter() {
        System.setProperty("useFilter", "false");

        doLoop("without");
    }

    @Test
    public void testWithNameFilter() {
        System.setProperty("useFilter", "true");

        doLoop("with");
    }

    private void doLoop(String name) {

        // warmup
        try (GenericContainer container = new GenericContainer<>("alpine:3.2").withCommand("date")) {
            container.start();
        }

        // run
        Stopwatch stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < 50; i++) {
            RemoteDockerImage.AVAILABLE_IMAGE_NAME_CACHE.clear();

            try (GenericContainer container = new GenericContainer<>("alpine:3.2").withCommand("date")) {
                container.start();
            }
        }
        stopwatch.stop();
        System.err.printf("%20s: %s \n", name, stopwatch.elapsed(TimeUnit.SECONDS));
    }
}
