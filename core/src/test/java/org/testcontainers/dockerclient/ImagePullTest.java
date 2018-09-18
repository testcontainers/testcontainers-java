package org.testcontainers.dockerclient;

import org.apache.commons.lang.SystemUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.junit.Assume.assumeTrue;

@RunWith(Parameterized.class)
public class ImagePullTest {

    private String image;
    private boolean isWindows;

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return new Object[][] {
            new Object[] {"alpine:latest", false},
            new Object[] {"alpine:3.6", false},
            new Object[] {"alpine", false}, // omitting the tag should work and default to latest
            new Object[] {"alpine@sha256:8fd4b76819e1e5baac82bd0a3d03abfe3906e034cc5ee32100d12aaaf3956dc7", false},
            new Object[] {"gliderlabs/alpine:latest", false},
            new Object[] {"gliderlabs/alpine:3.5", false},
            new Object[] {"gliderlabs/alpine@sha256:a19aa4a17a525c97e5a90a0c53a9f3329d2dc61b0a14df5447757a865671c085", false},
            new Object[] {"quay.io/testcontainers/ryuk:latest", false},
            new Object[] {"quay.io/testcontainers/ryuk:0.2.2", false},
            new Object[] {"quay.io/testcontainers/ryuk@sha256:4b606e54c4bba1af4fd814019d342e4664d51e28d3ba2d18d24406edbefd66da", false},
            new Object[] {"microsoft/nanoserver", true},
        };
    }

    public ImagePullTest(String image, boolean isWindows) {
        this.image = image;
        this.isWindows = isWindows;
    }

    @Test
    public void test() {
        if (isWindows) {
            assumeTrue(SystemUtils.IS_OS_WINDOWS);
        } else {
            assumeTrue(SystemUtils.IS_OS_UNIX);
        }

        try (final GenericContainer container = new GenericContainer<>(image)
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {
            if (isWindows) {
                container.withCommand("cmd", "/c", "ping localhost -n 1");
            } else {
                container.withCommand("/bin/sh", "-c", "sleep 0");
            }
            container.start();
            // do nothing other than start and stop
        }
    }
}
