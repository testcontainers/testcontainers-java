package org.testcontainers.dockerclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

@RunWith(Parameterized.class)
public class ImagePullTest {

    private String image;

    @Parameterized.Parameters(name = "{0}")
    public static String[] parameters() {
        return new String[] {
            "alpine:latest",
            "alpine:3.6",
            "alpine@sha256:8fd4b76819e1e5baac82bd0a3d03abfe3906e034cc5ee32100d12aaaf3956dc7",
            "gliderlabs/alpine:latest",
            "gliderlabs/alpine:3.5",
            "gliderlabs/alpine@sha256:a19aa4a17a525c97e5a90a0c53a9f3329d2dc61b0a14df5447757a865671c085",
            "quay.io/testcontainers/ryuk:latest",
            "quay.io/testcontainers/ryuk:0.2.2",
            "quay.io/testcontainers/ryuk@sha256:4b606e54c4bba1af4fd814019d342e4664d51e28d3ba2d18d24406edbefd66da"
        };
    }

    public ImagePullTest(String image) {
        this.image = image;
    }

    @Test
    public void test() {
        try (final GenericContainer container = new GenericContainer<>(image)
            .withCommand("/bin/sh", "-c", "sleep 0")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {
            container.start();
            // do nothing other than start and stop
        }
    }
}
