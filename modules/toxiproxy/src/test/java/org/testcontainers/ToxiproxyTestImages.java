package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface ToxiproxyTestImages {
    DockerImageName REDIS_IMAGE = new DockerImageName("redis:5.0.4");
    DockerImageName TOXIPROXY_IMAGE = new DockerImageName("shopify/toxiproxy:2.1.0");
}
