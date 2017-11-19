package org.testcontainers.containers;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.time.Duration;

@Getter
public class CephContainer<SELF extends CephContainer<SELF>> extends GenericContainer<SELF> {

    public static final String IMAGE = "ceph/demo";
    public static final int S3_PORT = 80;
    public static final int REST_API_PORT = 5000;

    private String awsAccessKey = "ceph";

    private String awsSecretKey = "ceph";

    private String bucketName = "CEPH";

    private String rgwName = "localhost";

    private String demoUid = "ceph";

    private NetworkAutoDetectMode networkAutoDetectMode = NetworkAutoDetectMode.IPV4_ONLY;

    public CephContainer() {
        super(IMAGE + ":tag-stable-3.0-jewel-ubuntu-16.04");
    }

    public CephContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        withEnv("RGW_NAME", rgwName);
        withEnv("NETWORK_AUTO_DETECT", networkAutoDetectMode.value);
        withEnv("CEPH_DEMO_UID", demoUid);
        withEnv("CEPH_DEMO_ACCESS_KEY", awsAccessKey);
        withEnv("CEPH_DEMO_SECRET_KEY", awsSecretKey);
        withEnv("CEPH_DEMO_BUCKET", bucketName);
        withExposedPorts(REST_API_PORT, S3_PORT);
        waitingFor(
                new HttpWaitStrategy()
                        .forPath("/api/v0.1/health")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(5))
        );
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(REST_API_PORT);
    }

    public AWSCredentialsProvider getAWSCredentialsProvider() {
        return new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(awsAccessKey, awsSecretKey)
        );
    }

    public AwsClientBuilder.EndpointConfiguration getAWSEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(
                getContainerIpAddress() + ":" + getMappedPort(S3_PORT),
                "us-east-1"
        );
    }

    public SELF withAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
        return self();
    }

    public SELF withAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
        return self();
    }

    public SELF withBucketName(String bucketName) {
        //because s3cmd transforming bucket name to uppercase
        this.bucketName = bucketName.toUpperCase();
        return self();
    }

    public SELF withRgwName(String rgwName) {
        this.rgwName = rgwName;
        return self();
    }

    public SELF withDemoUid(String demoUid) {
        this.demoUid = demoUid;
        return self();
    }

    public SELF withNetworkAutoDetectMode(NetworkAutoDetectMode networkAutoDetectMode) {
        this.networkAutoDetectMode = networkAutoDetectMode;
        return self();
    }

    @AllArgsConstructor
    public enum NetworkAutoDetectMode {
        IPV6_OR_IPV4("1"),
        IPV4_ONLY("4"),
        IPV6_ONLY("6");

        private final String value;

    }
}
