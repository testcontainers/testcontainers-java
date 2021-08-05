package org.testcontainers.containers;

import java.util.Collections;
import java.util.Set;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 * See <a href="https://store.docker.com/images/influxdb">https://store.docker.com/images/influxdb</a>
 */
public class InfluxDBContainerV1<SELF extends InfluxDBContainerV1<SELF>> extends GenericContainer<SELF> {

    public static final Integer INFLUXDB_PORT = 8086;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");
    private static final String DEFAULT_TAG = "1.4.3";

    private static final int NO_CONTENT_STATUS_CODE = 204;

    @Deprecated
    public static final String VERSION = DEFAULT_TAG;

    private boolean authEnabled = true;
    private String admin = "admin";
    private String adminPassword = "password";

    private String database;
    private String username = "any";
    private String password = "any";

    /**
     * @deprecated use {@link InfluxDBContainerV1(DockerImageName)} instead
     */
    @Deprecated
    public InfluxDBContainerV1(final String version) {
        this(DEFAULT_IMAGE_NAME.withTag(version));
    }

    public InfluxDBContainerV1(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        this.waitStrategy = new WaitAllStrategy()
            .withStrategy(Wait.forHttp("/ping").withBasicCredentials(this.username, this.password)
                .forStatusCode(NO_CONTENT_STATUS_CODE))
            .withStrategy(Wait.forListeningPort());

        this.addExposedPort(INFLUXDB_PORT);
    }

    @Override
    protected void configure() {
        this.addEnv("INFLUXDB_ADMIN_USER", this.admin);
        this.addEnv("INFLUXDB_ADMIN_PASSWORD", this.adminPassword);

        this.addEnv("INFLUXDB_HTTP_AUTH_ENABLED", String.valueOf(this.authEnabled));

        this.addEnv("INFLUXDB_DB", this.database);
        this.addEnv("INFLUXDB_USER", this.username);
        this.addEnv("INFLUXDB_USER_PASSWORD", this.password);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Collections.singleton(this.getMappedPort(INFLUXDB_PORT));
    }

    /**
     * Set env variable `INFLUXDB_HTTP_AUTH_ENABLED`.
     *
     * @param authEnabled Enables authentication.
     * @return a reference to this container instance
     */
    public SELF withAuthEnabled(final boolean authEnabled) {
        this.authEnabled = authEnabled;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_ADMIN_USER`.
     *
     * @param admin The name of the admin user to be created. If this is unset, no admin user is created.
     * @return a reference to this container instance
     */
    public SELF withAdmin(final String admin) {
        this.admin = admin;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_ADMIN_PASSWORD`.
     *
     * @param adminPassword TThe password for the admin user configured with `INFLUXDB_ADMIN_USER`. If this is unset, a
     * random password is generated and printed to standard out.
     * @return a reference to this container instance
     */
    public SELF withAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_DB`.
     *
     * @param database Automatically initializes a database with the name of this environment variable.
     * @return a reference to this container instance
     */
    public SELF withDatabase(final String database) {
        this.database = database;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_USER`.
     *
     * @param username The name of a user to be created with no privileges. If `INFLUXDB_DB` is set, this user will be
     * granted read and write permissions for that database.
     * @return a reference to this container instance
     */
    public SELF withUsername(final String username) {
        this.username = username;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_USER_PASSWORD`.
     *
     * @param password The password for the user configured with `INFLUXDB_USER`. If this is unset, a random password is
     * generated and printed to standard out.
     * @return a reference to this container instance
     */
    public SELF withPassword(final String password) {
        this.password = password;
        return this.self();
    }


    /**
     * @return a url to influxDb
     */
    public String getUrl() {
        return "http://" + this.getHost() + ":" + this.getMappedPort(INFLUXDB_PORT);
    }

    /**
     * @return a influxDb client
     */
    public InfluxDB getNewInfluxDB() {
        final InfluxDB influxDB = InfluxDBFactory.connect(this.getUrl(), this.username, this.password);
        influxDB.setDatabase(this.database);
        return influxDB;
    }
}
