package org.testcontainers.dockerclient;

import com.github.dockerjava.transport.SSLConfig;
import lombok.Builder;
import lombok.Value;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

@Builder
@Value
public class TransportConfig {

    URI dockerHost;

    @Nullable
    SSLConfig sslConfig;
}
