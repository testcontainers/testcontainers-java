package org.testcontainers.containers;

import static org.junit.Assert.assertEquals;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import lombok.SneakyThrows;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * See
 * <a href="https://docs.influxdata.com/influxdb/v2.0/get-started/#download-and-run-influxdb-v2-0">https://docs.influxdata.com/influxdb/v2.0/get-started/#download-and-run-influxdb-v2-0</a>
 */
public class InfluxDBContainerV2<SELF extends InfluxDBContainerV2<SELF>> extends GenericContainer<SELF> {

    public static final Integer INFLUXDB_PORT = 8086;

    private static final String REGISTRY = "quay.io";
    private static final String REPOSITORY = "influxdb/influxdb";
    private static final String TAG = "v2.0.0";
    private static final DockerImageName DEFAULT_IMAGE_NAME =
        DockerImageName.parse(String.format("%s/%s:%s", REGISTRY, REPOSITORY, TAG));
    private static final int NO_CONTENT_STATUS_CODE = 204;
    private static final String INFLUX_SETUP_SH = "influx-setup.sh";

    private String username = "test-user";
    private String password = "test-password";
    private String bucket = "test-bucket";
    private String organization = "test-org";
    private int retention = 0;
    private String retentionUnit = RetentionUnit.NANOSECONDS.label;

    private InfluxDBContainerV2(final DockerImageName imageName) {
        super(imageName);
        imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.waitStrategy = (new WaitAllStrategy())
            .withStrategy(Wait
                .forHttp("/ping")
                .withBasicCredentials(this.username, this.password)
                .forStatusCode(NO_CONTENT_STATUS_CODE))
            .withStrategy(Wait.forListeningPort());
        this.addExposedPort(INFLUXDB_PORT);
    }

    public static InfluxDBContainerV2<?> createWithDefaultTag() {
        return new InfluxDBContainerV2<>(DEFAULT_IMAGE_NAME);
    }

    public static InfluxDBContainerV2<?> createWithSpecificTag(final DockerImageName imageName) {
        return new InfluxDBContainerV2<>(imageName);
    }

    @Override
    protected void configure() {
        this.addEnv("INFLUXDB_USER", this.username);
        this.addEnv("INFLUXDB_PASSWORD", this.password);
        this.addEnv("INFLUXDB_BUCKET", this.bucket);
        this.addEnv("INFLUXDB_ORG", this.organization);
        this.addEnv("INFLUXDB_RETENTION", String.valueOf(this.retention));
        this.addEnv("INFLUXDB_RETENTION_UNIT", this.retentionUnit);
    }

    @Override
    @SneakyThrows({InterruptedException.class, IOException.class, ExecutionException.class})
    public void start() {
        this.withCopyFileToContainer(MountableFile.forClasspathResource(INFLUX_SETUP_SH),
            String.format("%s", INFLUX_SETUP_SH));
        if (this.containerId != null) {
            return;
        }
        Startables.deepStart(this.dependencies).get();
        // trigger LazyDockerClient's resolve so that we fail fast here and not in getDockerImageName()
        this.dockerClient.authConfig();
        this.doStart();
        final Container.ExecResult execResult = this.execInContainer("chmod", "-x", "/influx-setup.sh");
        assertEquals(execResult.getExitCode(), 0);
        final Container.ExecResult writeResult = this.execInContainer("/bin/bash", "/influx-setup.sh");
        assertEquals(writeResult.getExitCode(), 0);
    }

    /**
     * Set env variable `INFLUXDB_USER`.
     *
     * @param username The name of a user to be created with no privileges. If `INFLUXDB_BUCKET` is set, this user will
     * be granted read and write permissions for that database.
     * @return a reference to this container instance
     */
    public SELF withUsername(final String username) {
        this.username = username;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_PASSWORD`.
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
     * Set env variable `INFLUXDB_BUCKET`.
     *
     * @param bucket Automatically initializes a bucket with the name of this environment variable.
     * @return a reference to this container instance
     */
    public SELF withBucket(final String bucket) {
        this.bucket = bucket;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_ORGANIZATION`.
     *
     * @param organization The organization for the initial setup of influxDB.
     * @return a reference to this container instance
     */
    public SELF withOrganization(final String organization) {
        this.organization = organization;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_RETENTION`.
     *
     * @param retention Duration bucket will retain data (0 is infinite, default is 0).
     * @return a reference to this container instance
     */
    public SELF withRetention(final int retention) {
        this.retention = retention;
        return this.self();
    }

    /**
     * Set env variable `INFLUXDB_RETENTION_UNIT`.
     *
     * @param retentionUnit The retention unit (ns, us, ms, etc.).
     * @return a reference to this container instance
     */
    public SELF withRetentionUnit(final RetentionUnit retentionUnit) {
        this.retentionUnit = retentionUnit.label;
        return this.self();
    }


    /**
     * @return a influxDb client
     */
    public InfluxDBClient getNewInfluxDB() {
        final InfluxDBClientOptions influxDBClientOptions = InfluxDBClientOptions.builder()
            .url(this.getUrl())
            .authenticate(this.username, this.password.toCharArray())
            .bucket(this.bucket)
            .org(this.organization)
            .build();
        return InfluxDBClientFactory.create(influxDBClientOptions);
    }

    /**
     * @return a url to influxDb
     */
    String getUrl() {
        return "http://" + this.getHost() + ":" + this.getMappedPort(INFLUXDB_PORT);
    }
}
