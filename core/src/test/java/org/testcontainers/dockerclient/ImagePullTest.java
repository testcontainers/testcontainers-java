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
            "alpine:3.14",
            "alpine", // omitting the tag should work and default to latest
            "alpine@sha256:1775bebec23e1f3ce486989bfc9ff3c4e951690df84aa9f926497d82f2ffca9d",
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
