package org.testcontainers.consul;

import org.testcontainers.utility.DockerImageName;

public interface ConsulTestImages {
    DockerImageName CONSUL_IMAGE = DockerImageName.parse("consul:1.10.12");
}
