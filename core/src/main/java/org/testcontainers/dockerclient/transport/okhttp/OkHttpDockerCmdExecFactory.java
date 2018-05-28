package org.testcontainers.dockerclient.transport.okhttp;

import com.github.dockerjava.api.command.PingCmd;
import com.github.dockerjava.core.AbstractDockerCmdExecFactory;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.SSLConfig;
import com.github.dockerjava.core.WebTarget;
import com.github.dockerjava.core.exec.PingCmdExec;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.MultimapBuilder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.internal.Internal;
import org.apache.commons.io.IOUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OkHttpDockerCmdExecFactory extends AbstractDockerCmdExecFactory {

    private static final String SOCKET_SUFFIX = ".socket";

    private OkHttpClient okHttpClient;

    private HttpUrl baseUrl;

    @Override
    @SneakyThrows
    public void init(DockerClientConfig dockerClientConfig) {
        super.init(dockerClientConfig);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true);

        URI dockerHost = dockerClientConfig.getDockerHost();
        switch (dockerHost.getScheme()) {
            case "unix":
            case "npipe":
                String socketPath = dockerHost.getPath();

                if ("unix".equals(dockerHost.getScheme())) {
                    clientBuilder
                        .socketFactory(new UnixSocketFactory(socketPath));
                } else {
                    clientBuilder
                        .socketFactory(new NamedPipeSocketFactory(socketPath));
                }

                clientBuilder
                    // Disable pooling
                    .connectionPool(new ConnectionPool(0, 1, TimeUnit.SECONDS))
                    .dns(hostname -> {
                        if (hostname.endsWith(SOCKET_SUFFIX)) {
                            return Collections.singletonList(InetAddress.getByAddress(hostname, new byte[]{0, 0, 0, 0}));
                        } else {
                            return Dns.SYSTEM.lookup(hostname);
                        }
                    });
            default:
        }

        SSLConfig sslConfig = dockerClientConfig.getSSLConfig();
        if (sslConfig != null) {
            SSLContext sslContext = sslConfig.getSSLContext();
            if (sslContext != null) {
                clientBuilder
                    .sslSocketFactory(sslContext.getSocketFactory(), new TrustAllX509TrustManager());
            }
        }

        okHttpClient = clientBuilder.build();

        HttpUrl.Builder baseUrlBuilder;

        switch (dockerHost.getScheme()) {
            case "unix":
            case "npipe":
                baseUrlBuilder = new HttpUrl.Builder()
                    .scheme("http")
                    .host("docker" + SOCKET_SUFFIX);
                break;
            case "tcp":
                baseUrlBuilder = new HttpUrl.Builder()
                    .scheme(sslConfig != null && sslConfig.getSSLContext() != null ? "https" : "http")
                    .host(dockerHost.getHost())
                    .port(dockerHost.getPort());
                break;
            default:
                baseUrlBuilder = Internal.instance.getHttpUrlChecked(dockerHost.toString()).newBuilder();
        }
        baseUrl = baseUrlBuilder.build();
    }

    @Override
    protected WebTarget getBaseResource() {
        return new OkHttpWebTarget(
            okHttpClient,
            baseUrl,
            ImmutableList.of(),
            MultimapBuilder.hashKeys().hashSetValues().build()
        );
    }

    @Override
    public PingCmd.Exec createPingCmdExec() {
        return new PingCmdExec(getBaseResource(), getDockerClientConfig()) {

            @Override
            protected Void execute(PingCmd command) {
                WebTarget webResource = getBaseResource().path("/_ping");

                // TODO contribute to docker-java, make it close the stream
                IOUtils.closeQuietly(webResource.request().get());

                return null;
            }
        };
    }

    @Override
    public void close() throws IOException {

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
