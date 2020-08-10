package org.testcontainers.dockerclient;

import com.github.dockerjava.transport.SSLConfig;
import lombok.Builder;
import lombok.Value;

import java.net.URI;

@Builder
@Value
public class TransportConfig {

    URI dockerHost;

    SSLConfig sslConfig;
}
