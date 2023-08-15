package org.testcontainers.dockerclient;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

public class ImagePullTest {

    public static Stream<Arguments> provideImages() {
        return Stream.of(
            Arguments.of("alpine:latest"),
            Arguments.of("alpine:3.16"),
            Arguments.of("alpine"), // omitting the tag should work and default to latest
            Arguments.of("alpine@sha256:1775bebec23e1f3ce486989bfc9ff3c4e951690df84aa9f926497d82f2ffca9d"),
            Arguments.of("quay.io/testcontainers/ryuk:latest"),
            Arguments.of("quay.io/testcontainers/ryuk:0.2.3"),
            Arguments.of(
                "quay.io/testcontainers/ryuk@sha256:bb5a635cac4bd96c93cc476969ce11dc56436238ec7cd028d0524462f4739dd9"
            )
            //            Arguments.of("ibmcom/db2express-c")  // Big image for testing with slow network
        );
    }

    @ParameterizedTest
    @MethodSource("provideImages")
    public void test(String image) {
        try (
            final GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image))
                .withCommand("/bin/sh", "-c", "sleep 0")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
            // do nothing other than start and stop
        }
    }
}
