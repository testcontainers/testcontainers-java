package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * @deprecated instead use {@link InfluxDBContainerV1} for InfluxDB 1.x or {@link InfluxDBContainerV2} for InfluxDB 2.x instead
 */
@Deprecated
public class InfluxDBContainer<SELF extends InfluxDBContainer<SELF>> extends InfluxDBContainerV1<SELF> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("influxdb");
    private static final String DEFAULT_TAG = "1.4.3";

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
     * @param adminPassword TThe password for the admin user configured with `INFLUXDB_ADMIN_USER`. If this is unset, a
     * random password is generated and printed to standard out.
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
     * granted read and write permissions for that database.
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
     * generated and printed to standard out.
     * @return a reference to this container instance
     */
    @Override
    public SELF withPassword(final String password) {
        return (SELF) this;
    }
}
