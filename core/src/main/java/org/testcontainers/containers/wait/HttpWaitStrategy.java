package org.testcontainers.containers.wait;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Waits until an HTTP(S) endpoint returns a given status code.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HttpWaitStrategy extends GenericContainer.AbstractWaitStrategy {
    /**
     * Authorization HTTP header.
     */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Basic Authorization scheme prefix.
     */
    private static final String AUTH_BASIC = "Basic ";

    private String path = "/";
    private int statusCode = HttpURLConnection.HTTP_OK;
    private boolean tlsEnabled;
    private String username;
    private String password;

    /**
     * Waits for the given status code.
     *
     * @param statusCode the expected status code
     * @return this
     */
    public HttpWaitStrategy forStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Waits for the given path.
     *
     * @param path the path to check
     * @return this
     */
    public HttpWaitStrategy forPath(String path) {
        this.path = path;
        return this;
    }

    /**
     * Indicates that the status check should use HTTPS.
     *
     * @return this
     */
    public HttpWaitStrategy usingTls() {
        this.tlsEnabled = true;
        return this;
    }

    /**
     * Authenticate with HTTP Basic Authorization credentials.
     *
     * @param username the username
     * @param password the password
     * @return this
     */
    public HttpWaitStrategy withBasicCredentials(String username, String password) {
        this.username = username;
        this.password = password;
        return this;
    }

    @Override
    protected void waitUntilReady() {
        final Integer livenessCheckPort = getLivenessCheckPort();
        if (null == livenessCheckPort) {
            logger().warn("No exposed ports or mapped ports - cannot wait for status");
            return;
        }

        final String uri = buildLivenessUri(livenessCheckPort).toString();
        logger().info("Waiting for {} seconds for URL: {}", startupTimeout.getSeconds(), uri);

        // try to connect to the URL
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try {
                        final HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();

                        // authenticate
                        if (!Strings.isNullOrEmpty(username)) {
                            connection.setRequestProperty(HEADER_AUTHORIZATION, buildAuthString(username, password));
                            connection.setUseCaches(false);
                        }

                        connection.setRequestMethod("GET");
                        connection.connect();

                        if (statusCode != connection.getResponseCode()) {
                            throw new RuntimeException(String.format("HTTP response code was: %s",
                                    connection.getResponseCode()));
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException(String.format(
                    "Timed out waiting for URL to be accessible (%s should return HTTP %s)", uri, statusCode));
        }
    }

    /**
     * Build the URI on which to check if the container is ready.
     *
     * @param livenessCheckPort the liveness port
     * @return the liveness URI
     */
    private URI buildLivenessUri(int livenessCheckPort) {
        final String scheme = (tlsEnabled ? "https" : "http") + "://";
        final String host = container.getContainerIpAddress();

        final String portSuffix;
        if ((tlsEnabled && 443 == livenessCheckPort) || (!tlsEnabled && 80 == livenessCheckPort)) {
            portSuffix = "";
        } else {
            portSuffix = ":" + String.valueOf(livenessCheckPort);
        }

        return URI.create(scheme + host + portSuffix + path);
    }

    /**
     * @param username the username
     * @param password the password
     * @return a basic authentication string for the given credentials
     */
    private String buildAuthString(String username, String password) {
        return AUTH_BASIC + BaseEncoding.base64().encode((username + ":" + password).getBytes());
    }
}
