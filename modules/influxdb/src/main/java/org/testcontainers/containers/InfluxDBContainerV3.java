package org.testcontainers.containers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.HttpResponseException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpPost;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClients;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;

/**
 * Testcontainers implementation for InfluxDB 3 (InfluxDB IOx).
 * <p>
 * This container provides a instance of InfluxDB 3.x for integration testing.
 * It supports both authenticated and non-authenticated modes.
 * </p>
 *
 * <p>
 * <strong>Example usage:</strong>
 * <pre>{@code
 * try (InfluxDBContainerV3<?> influxDB = new InfluxDBContainerV3<>("influxdb:3-core")) {
 *     influxDB.start();
 *     String url = influxDB.getUrl();
 *     String token = influxDB.getToken();
 *     // Use InfluxDB client with the obtained URL and token
 * }
 * }</pre>
 * </p>
 */
public class InfluxDBContainerV3<SELF extends InfluxDBContainerV3<SELF>> extends GenericContainer<SELF> {

    /**
     * The default port exposed by InfluxDB 3.
     */
    public static final Integer INFLUXDB_PORT = 8181;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");

    /**
     * The authentication token for InfluxDB 3. Lazily initialized and thread-safe.
     */
    private volatile String token;

    /**
     * Flag indicating whether authentication is disabled.
     */
    private boolean isAuthDisable;

    /**
     * Creates a new InfluxDB 3 container using the specified Docker image.
     *
     * @param dockerImageName the name of the Docker image
     */
    public InfluxDBContainerV3(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy =
            new HttpWaitStrategy()
                .forPath("/health")
                .forStatusCodeMatching(stausCode -> stausCode.equals(200) || stausCode.equals(401));

        withCommand("influxdb3 serve --node-id local01 --object-store file --data-dir /home/influxdb3/.influxdb3");

        addExposedPort(INFLUXDB_PORT);
    }

    /**
     * Creates an admin authentication token by making an HTTP request to the InfluxDB 3 instance.
     *
     * @return the generated authentication token
     * @throws IllegalArgumentException if the token cannot be created due to HTTP or IO errors
     * @throws HttpResponseException if the InfluxDB server returns a non-201 status code
     */
    private String createToken() {
        HttpPost httpPost = new HttpPost(format("%s/api/v3/configure/token/admin", getUrl()));

        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            return httpClient.execute(httpPost, classicHttpResponse -> {
                if (classicHttpResponse.getCode() != HttpStatus.SC_CREATED) {
                    throw new HttpResponseException(
                        classicHttpResponse.getCode(),
                        "Failed to get token"
                    );
                }
                try (InputStream content = classicHttpResponse.getEntity().getContent()) {
                    return new ObjectMapper().readTree(content).get("token").asText();
                }
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot get token", e);
        }
    }

    /**
     * Configures environment variables for the InfluxDB 3 container.
     * <p>
     * This is automatically called by Testcontainers during container startup.
     * </p>
     */
    @Override
    protected void configure() {
        addEnv("INFLUXDB3_START_WITHOUT_AUTH", Boolean.toString(isAuthDisable));
    }

    /**
     * @return a singleton set containing the mapped InfluxDB port
     */
    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(INFLUXDB_PORT));
    }

    /**
     * Disables authentication for this InfluxDB instance.
     * <p>
     * When authentication is disabled, no token is required to access the database.
     * </p>
     *
     * @return this container instance for method chaining
     */
    public InfluxDBContainerV3<SELF> withDisableAuth() {
        isAuthDisable = true;
        return this;
    }

    /**
     * Gets the URL for connecting to this InfluxDB instance.
     * <p>
     * The URL includes the host and mapped port (since the actual port may change).
     * </p>
     *
     * @return the HTTP URL to access InfluxDB (e.g., "http://localhost:32768")
     */
    public String getUrl() {
        return "http://" + getHost() + ":" + getMappedPort(INFLUXDB_PORT);
    }

    /**
     * Gets the authentication token for this InfluxDB instance.
     * <p>
     * The token is lazily initialized on first use and cached for subsequent calls.
     * This method is thread-safe.
     * </p>
     *
     * @return the authentication token
     * @throws IllegalArgumentException if authentication is disabled or token creation fails
     */
    public String getToken() {
        String localToken = token;
        if (localToken == null) {
            synchronized (this) {
                localToken = token;
                if (localToken == null) {
                    token = localToken = createToken();
                }
            }
        }
        return localToken;
    }
}
