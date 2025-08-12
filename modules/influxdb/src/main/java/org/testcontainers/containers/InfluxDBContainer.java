package org.testcontainers.containers;

import lombok.Getter;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.ComparableVersion;
import org.testcontainers.utility.DockerImageName;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Testcontainers implementation for InfluxDB.
 * <p>
 * Supported image: {@code influxdb}
 * <p>
 * Exposed ports: 8086
 */
public class InfluxDBContainer<SELF extends InfluxDBContainer<SELF>> extends GenericContainer<SELF> {

    public static final Integer INFLUXDB_PORT = 8086;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");

    private static final int NO_CONTENT_STATUS_CODE = 204;

    @Getter
    private String username = "test-user";

    @Getter
    private String password = "test-password";

    /**
     * Properties of InfluxDB 1.x
     */
    private boolean authEnabled = true;

    private String admin = "admin";

    private String adminPassword = "password";

    @Getter
    private String database;

    /**
     * Properties of InfluxDB 2.x
     */
    @Getter
    private String bucket = "test-bucket";

    @Getter
    private String organization = "test-org";

    @Getter
    private Optional<String> retention = Optional.empty();

    @Getter
    private Optional<String> adminToken = Optional.empty();

    private final boolean isAtLeastMajorVersion2;

    /**
     * @deprecated use {@link #InfluxDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public InfluxDBContainer(final String version) {
        this(DEFAULT_IMAGE_NAME.withTag(version));
    }

    public InfluxDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy =
            new HttpWaitStrategy()
                .forPath("/ping")
                .withBasicCredentials(this.username, this.password)
                .forStatusCode(NO_CONTENT_STATUS_CODE);

        this.isAtLeastMajorVersion2 =
            new ComparableVersion(dockerImageName.getVersionPart()).isGreaterThanOrEqualTo("2.0.0");
        addExposedPort(INFLUXDB_PORT);
    }

    /**
     * Sets the InfluxDB environment variables based on the version
     */
    @Override
    protected void configure() {
        if (this.isAtLeastMajorVersion2) {
            configureInfluxDBV2();
        } else {
            configureInfluxDBV1();
        }
    }

    /**
     * Sets the InfluxDB 2.x environment variables
     *
     * @see <a href="https://hub.docker.com/_/influxdb"> InfluxDB Dockerhub </a> for full documentation on InfluxDB's
     * envrinoment variables</a>
     */
    private void configureInfluxDBV2() {
        addEnv("DOCKER_INFLUXDB_INIT_MODE", "setup");

        addEnv("DOCKER_INFLUXDB_INIT_USERNAME", this.username);
        addEnv("DOCKER_INFLUXDB_INIT_PASSWORD", this.password);

        addEnv("DOCKER_INFLUXDB_INIT_ORG", this.organization);
        addEnv("DOCKER_INFLUXDB_INIT_BUCKET", this.bucket);

        this.retention.ifPresent(ret -> addEnv("DOCKER_INFLUXDB_INIT_RETENTION", ret));
        this.adminToken.ifPresent(token -> addEnv("DOCKER_INFLUXDB_INIT_ADMIN_TOKEN", token));
    }

    /**
     * Sets the InfluxDB 1.x environment variables
     */
    private void configureInfluxDBV1() {
        addEnv("INFLUXDB_USER", this.username);
        addEnv("INFLUXDB_USER_PASSWORD", this.password);

        addEnv("INFLUXDB_HTTP_AUTH_ENABLED", String.valueOf(this.authEnabled));

        addEnv("INFLUXDB_ADMIN_USER", this.admin);
        addEnv("INFLUXDB_ADMIN_PASSWORD", this.adminPassword);

        addEnv("INFLUXDB_DB", this.database);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(INFLUXDB_PORT));
    }

    /**
     * Set user for InfluxDB
     *
     * @param username The username to set for the system's initial super-user
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withUsername(final String username) {
        this.username = username;
        return this;
    }

    /**
     * Set password for InfluxDB
     *
     * @param password The password to set for the system's initial super-user
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withPassword(final String password) {
        this.password = password;
        return this;
    }

    /**
     * Determines if authentication should be enabled or not
     *
     * @param authEnabled Enables authentication.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withAuthEnabled(final boolean authEnabled) {
        this.authEnabled = authEnabled;
        return this.self();
    }

    /**
     * Sets the admin user
     *
     * @param admin The name of the admin user to be created. If this is unset, no admin user is created.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withAdmin(final String admin) {
        this.admin = admin;
        return this.self();
    }

    /**
     * Sets the admin password
     *
     * @param adminPassword The password for the admin user. If this is unset, a random password is generated and
     * printed to standard out.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
        return this.self();
    }

    /**
     * Initializes database with given name
     *
     * @param database name of the database.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withDatabase(final String database) {
        this.database = database;
        return this.self();
    }

    /**
     * Sets the organization name
     *
     * @param organization The organization for the initial setup of influxDB.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withOrganization(final String organization) {
        this.organization = organization;
        return this;
    }

    /**
     * Initializes bucket with given name
     *
     * @param bucket name of the bucket.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withBucket(final String bucket) {
        this.bucket = bucket;
        return this;
    }

    /**
     * Sets the retention in days
     *
     * @param retention days bucket will retain data (0 is infinite, default is 0).
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withRetention(final String retention) {
        this.retention = Optional.of(retention);
        return this;
    }

    /**
     * Sets the admin token
     *
     * @param adminToken Authentication token to associate with the admin user.
     * @return a reference to this container instance
     */
    public InfluxDBContainer<SELF> withAdminToken(final String adminToken) {
        this.adminToken = Optional.of(adminToken);
        return this;
    }

    /**
     * @return a url to InfluxDB
     */
    public String getUrl() {
        return "http://" + getHost() + ":" + getMappedPort(INFLUXDB_PORT);
    }

    /**
     * @return a InfluxDB client for InfluxDB 1.x.
     * @deprecated Use the new <a href="https://github.com/influxdata/influxdb-client-java">InfluxDB client library.</a>
     */
    @Deprecated
    public InfluxDB getNewInfluxDB() {
        final InfluxDB influxDB = InfluxDBFactory.connect(getUrl(), this.username, this.password);
        influxDB.setDatabase(this.database);
        return influxDB;
    }
}
