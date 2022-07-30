package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * @deprecated instead use {@link InfluxDBContainerV1} for InfluxDB 1.x or {@link InfluxDBContainerV2} for InfluxDB 2.x instead
 */
@Deprecated
public class InfluxDBContainer<SELF extends InfluxDBContainer<SELF>> extends InfluxDBContainerV1<SELF> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");

    private static final String DEFAULT_TAG = "1.4.3";

    @Deprecated
    public static final String VERSION = DEFAULT_TAG;

    private boolean authEnabled = true;

    private String admin = "admin";

    private String adminPassword = "password";

    private String database;

    private String username = "any";

    private String password = "any";

    /**
     * @deprecated use {@link InfluxDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public InfluxDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link InfluxDBContainer(DockerImageName)} instead
     */
    @Deprecated
    public InfluxDBContainer(final String version) {
        this(DEFAULT_IMAGE_NAME.withTag(version));
    }

    public InfluxDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        waitStrategy =
            new WaitAllStrategy()
                .withStrategy(Wait.forHttp("/ping").withBasicCredentials(username, password).forStatusCode(204))
                .withStrategy(Wait.forListeningPort());

        addExposedPort(INFLUXDB_PORT);
    }

    @Override
    protected void configure() {
        addEnv("INFLUXDB_ADMIN_USER", admin);
        addEnv("INFLUXDB_ADMIN_PASSWORD", adminPassword);

        addEnv("INFLUXDB_HTTP_AUTH_ENABLED", String.valueOf(authEnabled));

        addEnv("INFLUXDB_DB", database);
        addEnv("INFLUXDB_USER", username);
        addEnv("INFLUXDB_USER_PASSWORD", password);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(getMappedPort(INFLUXDB_PORT));
    public InfluxDBContainer(final DockerImageName influxdbTestImage) {
        super(influxdbTestImage);
    }

    /**
     * Set env variable `INFLUXDB_HTTP_AUTH_ENABLED`.
     *
     * @param authEnabled Enables authentication.
     * @return a reference to this container instance
     */
    @Override
    public SELF withAuthEnabled(final boolean authEnabled) {
        return (SELF) this;
    }

    /**
     * Set env variable `INFLUXDB_ADMIN_USER`.
     *
     * @param admin The name of the admin user to be created. If this is unset, no admin user is created.
     * @return a reference to this container instance
     */
    @Override
    public SELF withAdmin(final String admin) {
        return (SELF) this;
    }

    /**
     * Set env variable `INFLUXDB_ADMIN_PASSWORD`.
     *
     * @param adminPassword The password for the admin user configured with `INFLUXDB_ADMIN_USER`. If this is unset, a
     *                      random password is generated and printed to standard out.
     * @return a reference to this container instance
     */
    @Override
    public SELF withAdminPassword(final String adminPassword) {
        return (SELF) this;
    }

    /**
     * Set env variable `INFLUXDB_DB`.
     *
     * @param database Automatically initializes a database with the name of this environment variable.
     * @return a reference to this container instance
     */
    @Override
    public SELF withDatabase(final String database) {
        return (SELF) this;
    }

    /**
     * Set env variable `INFLUXDB_USER`.
     *
     * @param username The name of a user to be created with no privileges. If `INFLUXDB_DB` is set, this user will be
     *                 granted read and write permissions for that database.
     * @return a reference to this container instance
     */
    @Override
    public SELF withUsername(final String username) {
        return (SELF) this;
    }

    /**
     * Set env variable `INFLUXDB_USER_PASSWORD`.
     *
     * @param password The password for the user configured with `INFLUXDB_USER`. If this is unset, a random password is
     *                 generated and printed to standard out.
     * @return a reference to this container instance
     */
    @Override
    public SELF withPassword(final String password) {
        return (SELF) this;
        this.password = password;
        return self();
    }

    /**
     * @return a url to influxDb
     */
    public String getUrl() {
        return "http://" + getHost() + ":" + getLivenessCheckPort();
    }
}
