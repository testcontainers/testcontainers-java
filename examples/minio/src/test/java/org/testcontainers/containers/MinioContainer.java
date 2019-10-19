package com.testcontainers.minio;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

import java.time.Duration;

/**
 * Provides MinIO docker container which exposed bu default 9000 port
 *
 * @author Kirilenko Aleksandr
 */
public class MinioContainer extends GenericContainer<MinioContainer> {

    private static final int DEFAULT_PORT = 9000;
    private static final String DEFAULT_IMAGE = "minio/minio";
    private static final String DEFAULT_TAG = "edge";

    private static final String MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY";
    private static final String MINIO_SECRET_KEY = "MINIO_SECRET_KEY";

    private static final String DEFAULT_STORAGE_DIRECTORY = "/data";
    private static final String HEALTH_ENDPOINT = "/minio/health/ready";

    public MinioContainer(CredentialsProvider credentials) {
        this(DEFAULT_IMAGE + ":" + DEFAULT_TAG, credentials);
    }

    public MinioContainer(String image, CredentialsProvider credentials) {
        super(image == null ? DEFAULT_IMAGE + ":" + DEFAULT_TAG : image);
        logger().info("Starting a minio container based on image: ", image);
        withNetworkAliases("minio-" + Base58.randomString(6));
        addExposedPort(DEFAULT_PORT);
        if (credentials != null) {
            withEnv(MINIO_ACCESS_KEY, credentials.getAccessKey());
            withEnv(MINIO_SECRET_KEY, credentials.getSecretKey());
        }
        withCommand("server", DEFAULT_STORAGE_DIRECTORY);
        setWaitStrategy(new HttpWaitStrategy()
            .forPort(DEFAULT_PORT)
            .forPath(HEALTH_ENDPOINT)
            .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public String getHostAddress() {
        return getContainerIpAddress() + ":" + getMappedPort(DEFAULT_PORT);
    }

    public static class CredentialsProvider {
        private String accessKey;
        private String secretKey;

        public CredentialsProvider(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }
    }
}
