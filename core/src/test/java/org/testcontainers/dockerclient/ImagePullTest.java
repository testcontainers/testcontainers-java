package org.testcontainers.dockerclient;

import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

public class ImagePullTest {

    @Test
    public void pullOfficialLatestImageTest() {
        doPullStartAndStop("alpine:latest");
    }

    @Test
    public void pullOfficialImageByTagTest() {
        doPullStartAndStop("alpine:3.6");
    }

    @Test
    public void pullOfficialImageByShaTest() {
        doPullStartAndStop("alpine@sha256:8fd4b76819e1e5baac82bd0a3d03abfe3906e034cc5ee32100d12aaaf3956dc7");
    }

    @Test
    public void pullLatestImageTest() {
        doPullStartAndStop("gliderlabs/alpine:latest");
    }

    @Test
    public void pullImageByTagTest() {
        doPullStartAndStop("gliderlabs/alpine:3.5");
    }

    @Test
    public void pullImageByShaTest() {
        doPullStartAndStop("gliderlabs/alpine@sha256:a19aa4a17a525c97e5a90a0c53a9f3329d2dc61b0a14df5447757a865671c085");
    }

    @Test
    public void pullLatestImageFromPublicRegistryTest() {
        doPullStartAndStop("quay.io/coreos/etcd:latest");
    }

    @Test
    public void pullImageByTagFromPublicRegistryTest() {
        doPullStartAndStop("quay.io/coreos/etcd:v3.1");
    }

    @Test
    public void pullImageByShaFromPublicRegistryTest() {
        doPullStartAndStop("quay.io/coreos/etcd@sha256:39a30367cd1f3186d540a063ea0257353c8f81b0d3c920c87c7e0f602bb6197c");
    }

    private void doPullStartAndStop(String s) {
        final GenericContainer container = new GenericContainer<>(s);
        container.start();
        container.stop();
    }
}
