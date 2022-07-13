package org.testcontainers.dockerclient;

import com.github.dockerjava.transport.DockerHttpClient;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.experimental.Delegate;

import java.io.Closeable;
import java.util.Map;

@RequiredArgsConstructor
@ToString
class HeadersAddingDockerHttpClient implements DockerHttpClient {

    @Delegate(types = Closeable.class)
    final DockerHttpClient delegate;

    final Map<String, String> headers;

    @Override
    public Response execute(Request request) {
        request = Request.builder().from(request).putAllHeaders(headers).build();
        return delegate.execute(request);
    }
}
