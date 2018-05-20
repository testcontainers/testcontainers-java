package org.testcontainers.dockerclient.transport.okhttp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.InvocationBuilder;
import com.github.dockerjava.core.SSLConfig;
import com.github.dockerjava.core.WebTarget;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.SetMultimap;
import de.gesellix.docker.client.filesocket.UnixSocket;
import de.gesellix.docker.client.filesocket.UnixSocketFactory;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.Wither;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.Internal;
import org.apache.commons.lang.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Wither
@Value
class OkHttpWebTarget implements WebTarget {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    DockerClientConfig dockerClientConfig;

    ImmutableList<String> path;

    SetMultimap<String, String> queryParams;

    @Override
    @SneakyThrows
    public InvocationBuilder request() {
        String resource = StringUtils.join(path, "/");

        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        SSLConfig sslConfig = dockerClientConfig.getSSLConfig();
        if (sslConfig != null) {
            SSLContext sslContext = sslConfig.getSSLContext();
            if (sslContext != null) {
                clientBuilder
                    .sslSocketFactory(sslContext.getSocketFactory(), new TrustAllX509TrustManager());
            }
        }

        HttpUrl.Builder urlBuilder;

        URI dockerHost = dockerClientConfig.getDockerHost();
        switch (dockerHost.getScheme()) {
            case "npipe":
                // TODO support it
                throw new IllegalArgumentException("npipe protocol is not supported yet");
            case "unix":
                UnixSocketFactory unixSocketFactory = new UnixSocketFactory();
                clientBuilder
                    .socketFactory(unixSocketFactory)
                    .dns(unixSocketFactory);

                urlBuilder = new HttpUrl.Builder()
                    .scheme("http")
                    .host(new UnixSocket().encodeHostname(dockerHost.getPath()));
                break;
            case "tcp":
                urlBuilder = new HttpUrl.Builder()
                    .scheme(sslConfig == null ? "http" : "https")
                    .host(dockerHost.getHost())
                    .port(dockerHost.getPort());
                break;
            default:
                urlBuilder = Internal.instance.getHttpUrlChecked(dockerHost.toString()).newBuilder();
        }

        urlBuilder
            .encodedPath(resource);

        for (Map.Entry<String, Collection<String>> queryParamEntry : queryParams.asMap().entrySet()) {
            String key = queryParamEntry.getKey();
            for (String paramValue : queryParamEntry.getValue()) {
                urlBuilder.addQueryParameter(key, paramValue);
            }
        }

        return new OkHttpInvocationBuilder(
            MAPPER,
            clientBuilder.build(),
            urlBuilder.build()
        );
    }

    @Override
    public OkHttpWebTarget path(String... components) {
        return this.withPath(
            ImmutableList.<String>builder()
                .addAll(path)
                .add(components)
                .build()
        );
    }

    @Override
    public OkHttpWebTarget resolveTemplate(String name, Object value) {
        ImmutableList.Builder<String> newPath = ImmutableList.builder();
        for (String component : path) {
            component = component.replaceAll("\\{" + name + "\\}", value.toString());
            newPath.add(component);
        }
        return this.withPath(newPath.build());
    }

    @Override
    public OkHttpWebTarget queryParam(String name, Object value) {
        if (value == null) {
            return this;
        }

        SetMultimap<String, String> newQueryParams = HashMultimap.create(queryParams);
        newQueryParams.put(name, value.toString());

        return this.withQueryParams(newQueryParams);
    }

    @Override
    public OkHttpWebTarget queryParamsSet(String name, Set<?> values) {
        SetMultimap<String, String> newQueryParams = HashMultimap.create(queryParams);
        newQueryParams.replaceValues(name, values.stream().filter(Objects::nonNull).map(Object::toString).collect(Collectors.toSet()));

        return this.withQueryParams(newQueryParams);
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public OkHttpWebTarget queryParamsJsonMap(String name, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return this;
        }

        // when param value is JSON string
        return queryParam(name, MAPPER.writeValueAsString(values));
    }

    private static class TrustAllX509TrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] x509Certificates, String s) {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] x509Certificates, String s) {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
