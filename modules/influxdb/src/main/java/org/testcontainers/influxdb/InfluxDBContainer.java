package org.testcontainers.influxdb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.HttpResponseException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.classic.methods.HttpPost;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.client5.http.impl.classic.HttpClients;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpStatus;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.String.format;

/**
 * Testcontainers implementation for InfluxDB 3 (InfluxDB IOx).
 * <p>
 * Supported image: {@code influxdb}
 * <p>
 * Exposed ports: 8181
 */
public class InfluxDBContainer extends GenericContainer<InfluxDBContainer> {

    /**
     * The default port exposed by InfluxDB 3.
     */
    private static final Integer INFLUXDB_PORT = 8181;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");

    /**
     * The authentication token for InfluxDB 3.
     */
    private String token;

    private boolean isAuthDisable;

    /**
     * Creates a new InfluxDB 3 container using the specified Docker image.
     *
     * @param dockerImageName the name of the Docker image
     */
    public InfluxDBContainer(final DockerImageName dockerImageName) {
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
     * @throws HttpResponseException    if the InfluxDB server returns a non-201 status code
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
     * Disables authentication for this InfluxDB instance.
     * <p>
     * When authentication is disabled, no token is required to access the database.
     * </p>
     *
     * @return this container instance for method chaining
     */
    public InfluxDBContainer withAuthDisabled() {
        this.isAuthDisable = true;
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

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        if (!isAuthDisable) {
            this.token = createToken();
        }
    }

    public String getToken() {
        return token;
    }
}
