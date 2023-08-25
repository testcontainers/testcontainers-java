package org.testcontainers.containers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Testcontainers implementation for MinIO.
 * <p>
 * Supported image: {@code MinIO}
 * <p>
 * Exposed ports: 9000,9001
 */
@Slf4j
public class MinIOContainer extends GenericContainer<MinIOContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("minio/minio");

    private static final String DEFAULT_TAG = "latest";

    private static final int MINIO_S3_PORT = 9000;

    private static final int MINIO_UI_PORT = 9001;

    private static final String DEFAULT_USER = "miniouser";

    private static final String DEFAULT_PASSWORD = "miniopassword";

    private String userName = DEFAULT_USER;

    private String password = DEFAULT_PASSWORD;

    /**
     * Constructs a MinIO container with the latest tag
     */
    public MinIOContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Constructs a MinIO container from the dockerImageName
     * @param dockerImageName the full image name to use
     */
    public MinIOContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Constructs a MinIO container from the dockerImageName
     * @param dockerImageName the full image name to use
     */
    public MinIOContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
    }

    /**
     * Overrides the DEFAULT_USER
     * @param userName the Root user to override
     * @return this
     */
    public MinIOContainer withUser(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * Overrides the DEFAULT_PASSWORD
     * @param password the Root user's password to override
     * @return this
     */
    public MinIOContainer withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Configures the MinIO container
     */
    @Override
    public void configure() {
        addFixedExposedPort(MinIOContainer.MINIO_S3_PORT, MinIOContainer.MINIO_S3_PORT);
        addFixedExposedPort(MinIOContainer.MINIO_UI_PORT, MinIOContainer.MINIO_UI_PORT);

        addEnv("MINIO_ROOT_USER", this.userName);
        addEnv("MINIO_ROOT_PASSWORD", this.password);

        withCommand("server", "--console-address", ":" + MINIO_UI_PORT, "/data");

        waitingFor(
            Wait
                .forLogMessage(".*Status:         1 Online, 0 Offline..*", 1)
                .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS))
        );
    }

    /**
     * @return the URL to upload/download objects from
     */
    public String getS3URL() {
        return String.format("http://%s:%s", this.getHost(), MINIO_S3_PORT);
    }

    /**
     * @return the URL to the Web Admin portal
     */
    public String getUIURL() {
        return String.format("http://%s:%s", this.getHost(), MINIO_UI_PORT);
    }

    /**
     * @return the Username for the Root user
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * @return the password for the Root user
     */
    public String getPassword() {
        return this.password;
    }
}
