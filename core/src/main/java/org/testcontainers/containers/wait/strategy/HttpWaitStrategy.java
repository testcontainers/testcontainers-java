package org.testcontainers.containers.wait.strategy;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.ContainerLaunchException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

@Slf4j
public class HttpWaitStrategy extends AbstractWaitStrategy {

    /**
     * Authorization HTTP header.
     */
    private static final String HEADER_AUTHORIZATION = "Authorization";

    /**
     * Basic Authorization scheme prefix.
     */
    private static final String AUTH_BASIC = "Basic ";

    private String path = "/";
    private String method = "GET";
    private Set<Integer> statusCodes = new HashSet<>();
    private boolean tlsEnabled;
    private String username;
    private String password;
    private final Map<String, String> headers = new HashMap<>();
    private Predicate<String> responsePredicate;
    private Predicate<Integer> statusCodePredicate = null;
    private Optional<Integer> livenessPort = Optional.empty();
    private Duration readTimeout = Duration.ofSeconds(1);

    /**
     * Waits for the given status code.
     *
     * @param statusCode the expected status code
     * @return this
     */
    public HttpWaitStrategy forStatusCode(int statusCode) {
        statusCodes.add(statusCode);
        return this;
    }

    /**
     * Waits for the status code to pass the given predicate
     * @param statusCodePredicate The predicate to test the response against
     * @return this
     */
    public HttpWaitStrategy forStatusCodeMatching(Predicate<Integer> statusCodePredicate) {
        this.statusCodePredicate = statusCodePredicate;
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
     * Wait for the given port.
     *
     * @param port the given port
     * @return this
     */
    public HttpWaitStrategy forPort(int port) {
        this.livenessPort = Optional.of(port);
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
     * Indicates the HTTP method to use (<code>GET</code> by default).
     *
     * @param method the HTTP method.
     * @return this
     */
    public HttpWaitStrategy withMethod(String method) {
        this.method = method;
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

    /**
     * Add a custom HTTP Header to the call.
     * @param name The HTTP Header name
     * @param value The HTTP Header value
     * @return this
     */
    public HttpWaitStrategy withHeader(String name, String value) {
        this.headers.put(name, value);
        return this;
    }

    /**
     * Add multiple custom HTTP Headers to the call.
     * @param headers Headers map of name/value
     * @return this
     */
    public HttpWaitStrategy withHeaders(Map<String, String> headers) {
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Set the HTTP connections read timeout.
     *
     * @param timeout the timeout (minimum 1 millisecond)
     * @return this
     */
    public HttpWaitStrategy withReadTimeout(Duration timeout) {
        if (timeout.toMillis() < 1) {
            throw new IllegalArgumentException("you cannot specify a value smaller than 1 ms");
        }
        this.readTimeout = timeout;
        return this;
    }

    /**
     * Waits for the response to pass the given predicate
     * @param responsePredicate The predicate to test the response against
     * @return this
     */
    public HttpWaitStrategy forResponsePredicate(Predicate<String> responsePredicate) {
        this.responsePredicate = responsePredicate;
        return this;
    }

    @Override
    protected void waitUntilReady() {
        final String containerName = waitStrategyTarget.getContainerInfo().getName();

        final Integer livenessCheckPort = livenessPort.map(waitStrategyTarget::getMappedPort).orElseGet(() -> {
            final Set<Integer> livenessCheckPorts = getLivenessCheckPorts();
            if (livenessCheckPorts == null || livenessCheckPorts.isEmpty()) {
                log.warn("{}: No exposed ports or mapped ports - cannot wait for status", containerName);
                return -1;
            }
            return livenessCheckPorts.iterator().next();
        });

        if (null == livenessCheckPort || -1 == livenessCheckPort) {
            return;
        }
        final URI rawUri = buildLivenessUri(livenessCheckPort);
        final String uri = rawUri.toString();

        try {
            // Un-map the port for logging
            int originalPort = waitStrategyTarget.getExposedPorts().stream()
                .filter(exposedPort -> rawUri.getPort() == waitStrategyTarget.getMappedPort(exposedPort))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Target port " + rawUri.getPort() + " is not exposed"));
            log.info("{}: Waiting for {} seconds for URL: {} (where port {} maps to container port {})", containerName, startupTimeout.getSeconds(), uri, rawUri.getPort(), originalPort);
        } catch (RuntimeException e) {
            // do not allow a failure in logging to prevent progress, but log for diagnosis
            log.warn("Unexpected error occurred - will proceed to try to wait anyway", e);
        }

        // try to connect to the URL
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try {
                        final HttpURLConnection connection = (HttpURLConnection) new URL(uri).openConnection();
                        connection.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));

                        // authenticate
                        if (!Strings.isNullOrEmpty(username)) {
                            connection.setRequestProperty(HEADER_AUTHORIZATION, buildAuthString(username, password));
                            connection.setUseCaches(false);
                        }

                        // Add user configured headers
                        this.headers.forEach(connection::setRequestProperty);
                        connection.setRequestMethod(method);
                        connection.connect();

                        log.trace("Get response code {}", connection.getResponseCode());

                        // Choose the statusCodePredicate strategy depending on what we defined.
                        Predicate<Integer> predicate;
                        if (statusCodes.isEmpty() && statusCodePredicate == null) {
                            // We have no status code and no predicate so we expect a 200 OK response code
                            predicate = responseCode -> HttpURLConnection.HTTP_OK == responseCode;
                        } else if (!statusCodes.isEmpty() && statusCodePredicate == null) {
                            // We use the default status predicate checker when we only have status codes
                            predicate = responseCode -> statusCodes.contains(responseCode);
                        } else if (statusCodes.isEmpty()) {
                            // We only have a predicate
                            predicate = statusCodePredicate;
                        } else {
                            // We have both predicate and status code
                            predicate = statusCodePredicate.or(responseCode -> statusCodes.contains(responseCode));
                        }
                        if (!predicate.test(connection.getResponseCode())) {
                            throw new RuntimeException(String.format("HTTP response code was: %s",
                                connection.getResponseCode()));
                        }

                        if(responsePredicate != null) {
                            String responseBody = getResponseBody(connection);

                            log.trace("Get response {}", responseBody);

                            if(!responsePredicate.test(responseBody)) {
                                throw new RuntimeException(String.format("Response: %s did not match predicate",
                                    responseBody));
                            }
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException(String.format(
                "Timed out waiting for URL to be accessible (%s should return HTTP %s)", uri, statusCodes.isEmpty() ?
                    HttpURLConnection.HTTP_OK : statusCodes));
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
        final String host = waitStrategyTarget.getHost();

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

    private String getResponseBody(HttpURLConnection connection) throws IOException {
        BufferedReader reader;
        if (200 <= connection.getResponseCode() && connection.getResponseCode() <= 299) {
            reader = new BufferedReader(new InputStreamReader((connection.getInputStream())));
        } else {
            reader = new BufferedReader(new InputStreamReader((connection.getErrorStream())));
        }

        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }
        return builder.toString();
    }
}
