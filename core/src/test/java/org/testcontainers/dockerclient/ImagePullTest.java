package org.testcontainers.dockerclient;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.GenericContainer;

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
            "quay.io/coreos/etcd:latest",
            "quay.io/coreos/etcd:v3.1",
            "quay.io/coreos/etcd@sha256:39a30367cd1f3186d540a063ea0257353c8f81b0d3c920c87c7e0f602bb6197c"
        };
    }

    public ImagePullTest(String image) {
        this.image = image;
    }

    @Test
    public void test() {
        try (final GenericContainer __ = new GenericContainer<>(image)) {
            // do nothing other than start and stop
        }
    }
}
