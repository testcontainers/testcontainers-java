package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Testcontainers implementation for MinIO.
 * <p>
 * Supported image: {@code minio/minio}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>S3: 9000</li>
 *     <li>Console: 9001</li>
 * </ul>
 */
public class MinIOContainer extends GenericContainer<MinIOContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("minio/minio");

    private static final int MINIO_S3_PORT = 9000;

    private static final int MINIO_UI_PORT = 9001;

    private static final String DEFAULT_USER = "minioadmin";

    private static final String DEFAULT_PASSWORD = "minioadmin";

    private String userName;

    private String password;

    /**
     * Constructs a MinIO container from the dockerImageName
     * @param dockerImageName the full image name to use
     */
    public MinIOContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Constructs a MinIO container from the dockerImageName
     * @param dockerImageName the full image name to use
     */
    public MinIOContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(MINIO_S3_PORT, MINIO_UI_PORT);
        withCommand("server", "--console-address", ":" + MINIO_UI_PORT, "/data");
        waitingFor(
            Wait
                .forHttp("/minio/health/live")
                .forPort(MINIO_S3_PORT)
                .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS))
        );
    }

    /**
     * Overrides the DEFAULT_USER
     * @param userName the Root user to override
     * @return this
     */
    public MinIOContainer withUserName(String userName) {
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
        if (this.userName != null) {
            addEnv("MINIO_ROOT_USER", this.userName);
        } else {
            this.userName = DEFAULT_USER;
        }
        if (this.password != null) {
            addEnv("MINIO_ROOT_PASSWORD", this.password);
        } else {
            this.password = DEFAULT_PASSWORD;
        }
    }

    /**
     * @return the URL to upload/download objects from
     */
    public String getS3URL() {
        return String.format("http://%s:%s", this.getHost(), getMappedPort(MINIO_S3_PORT));
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
