package org.testcontainers.dockerclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

@RunWith(Parameterized.class)
public class ImagePullTest {

    private final String image;

    @Parameterized.Parameters(name = "{0}")
    public static String[] parameters() {
        return new String[] {
            "alpine:latest",
            "alpine:3.6",
            "alpine", // omitting the tag should work and default to latest
            "alpine@sha256:8fd4b76819e1e5baac82bd0a3d03abfe3906e034cc5ee32100d12aaaf3956dc7",
            "gliderlabs/alpine:latest",
            "gliderlabs/alpine:3.5",
            "gliderlabs/alpine@sha256:a19aa4a17a525c97e5a90a0c53a9f3329d2dc61b0a14df5447757a865671c085",
            "quay.io/testcontainers/ryuk:latest",
            "quay.io/testcontainers/ryuk:0.2.3",
            "quay.io/testcontainers/ryuk@sha256:bb5a635cac4bd96c93cc476969ce11dc56436238ec7cd028d0524462f4739dd9",
//            "ibmcom/db2express-c", // Big image for testing with slow networks
        };
    }

    public ImagePullTest(String image) {
        this.image = image;
    }

    @Test
    public void test() {
        try (final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image))
            .withCommand("/bin/sh", "-c", "sleep 0")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())) {
            container.start();
            // do nothing other than start and stop
        }
    }
}
