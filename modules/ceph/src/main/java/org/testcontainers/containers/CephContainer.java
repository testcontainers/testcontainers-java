package org.testcontainers.containers;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;

@Slf4j
@Getter
public class CephContainer extends GenericContainer<CephContainer> {

    public static final String IMAGE = "ceph/daemon";
    public static final int S3_PORT = 8080;
    public static final int REST_API_PORT = 5000;

    private String awsAccessKey = "ceph";

    private String awsSecretKey = "ceph";

    private String bucketName = "CEPH";

    private String rgwName = "localhost";

    private String demoUid = "ceph";

    private NetworkAutoDetectMode networkAutoDetectMode = NetworkAutoDetectMode.IPV4_ONLY;

    public CephContainer() {
        super(IMAGE + ":v3.0.5-stable-3.0-luminous-centos-7");
    }

    public CephContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        withEnv("RGW_NAME", rgwName);
        withEnv("NETWORK_AUTO_DETECT", networkAutoDetectMode.value);
        withEnv("CEPH_DAEMON", "demo");
        withEnv("CEPH_DEMO_UID", demoUid);
        withEnv("CEPH_DEMO_ACCESS_KEY", awsAccessKey);
        withEnv("CEPH_DEMO_SECRET_KEY", awsSecretKey);
        withEnv("CEPH_DEMO_BUCKET", bucketName);
        withExposedPorts(REST_API_PORT, S3_PORT);
        waitingFor(
                new HttpWaitStrategy()
                        .forPath("/api/v0.1/health")
                        .forPort(REST_API_PORT)
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(5))
        );
        withLogConsumer(new Slf4jLogConsumer(log));
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

    public CephContainer withAwsAccessKey(String awsAccessKey) {
        this.awsAccessKey = awsAccessKey;
        return self();
    }

    public CephContainer withAwsSecretKey(String awsSecretKey) {
        this.awsSecretKey = awsSecretKey;
        return self();
    }

    public CephContainer withBucketName(String bucketName) {
        //because s3cmd transforming bucket name to uppercase
        this.bucketName = bucketName.toUpperCase();
        return self();
    }

    public CephContainer withRgwName(String rgwName) {
        this.rgwName = rgwName;
        return self();
    }

    public CephContainer withDemoUid(String demoUid) {
        this.demoUid = demoUid;
        return self();
    }

    public CephContainer withNetworkAutoDetectMode(NetworkAutoDetectMode networkAutoDetectMode) {
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
