package org.testcontainers.containers.wait;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Waits until an HTTP(S) endpoint returns a given status code.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HttpWaitStrategy extends GenericWaitStrategy<HttpWaitStrategy> {

    private static final Logger logger = LoggerFactory.getLogger(HttpWaitStrategy.class);

    /**
     * Authorization HTTP header.
     */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Basic Authorization scheme prefix.
     */
    private static final String AUTH_BASIC = "Basic ";

    private int port = 0;
    private String path = "/";
    private int statusCode = HttpURLConnection.HTTP_OK;
    private boolean tlsEnabled;
    private String username;
    private String password;

    public HttpWaitStrategy() {
        super("successful http request");
    }

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
     * Set request port.
     *
     * @param port the port to check on
     * @return this
     */
    public HttpWaitStrategy withPort(int port) {
        this.port = port;
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
    protected boolean isReady(GenericContainer container) throws Exception {

        final int readyPort = this.port != 0 ? container.getMappedPort(this.port) : getPrimaryMappedContainerPort(container).orElse(8080);

        final String uri = buildLivenessUri(container.getContainerIpAddress(), readyPort).toString();
        container.logger().info("Try to request " + uri);

        final HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();

        // authenticate
        if (!Strings.isNullOrEmpty(username)) {
            connection.setRequestProperty(HEADER_AUTHORIZATION, buildAuthString(username, password));
            connection.setUseCaches(false);
        }

        connection.setRequestMethod("GET");
        connection.connect();

        if (statusCode != connection.getResponseCode()) {
            container.logger().info("HTTP response code was " + connection.getResponseCode());
            return false;
        }

        return true;
    }

    /**
     * Build the URI on which to check if the container is ready.
     *
     * @param livenessCheckPort the liveness port
     * @return the liveness URI
     */
    private URI buildLivenessUri(String host, int livenessCheckPort) {
        final String scheme = (tlsEnabled ? "https" : "http") + "://";

        final String portSuffix;
        if ((tlsEnabled && 443 == livenessCheckPort) || (!tlsEnabled && 80 == livenessCheckPort)) {
            portSuffix = "";
        } else {
            portSuffix = ":" + livenessCheckPort;
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
