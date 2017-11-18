package org.testcontainers.containers;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;

import java.time.Duration;

public class CephContainer<SELF extends CephContainer<SELF>> extends GenericContainer<SELF> {

    private static final String IMAGE = "ceph/demo";

    private String awsAccessKey = "ceph";

    private String awsSecretKey = "ceph";

    private String bucketName = "CEPH";

    private String rgwName = "localhost";

    private String demoUid = "ceph";

    private NetworkAutoDetectMode networkAutoDetectMode = NetworkAutoDetectMode.IPV4_ONLY;

    private Duration startupTimeout = Duration.ofSeconds(20);

    public CephContainer() {
        super(IMAGE + ":latest");
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
        withExposedPorts(6789, 6800, 6801, 6802, 6803, 6804, 6805, 80, 5000);
        waitingFor(
                new LogMessageWaitStrategy()
                        .withRegEx(".*\\/entrypoint.sh: SUCCESS\n")
                        .withStartupTimeout(startupTimeout)
        );
    }

    public AWSCredentialsProvider getAWSCredentialsProvider() {
        return new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(awsAccessKey, awsSecretKey)
        );
    }

    public AwsClientBuilder.EndpointConfiguration getAWSEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration(
                getContainerIpAddress() + ":" + getMappedPort(80),
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

    public String getBucketName() {
        return bucketName;
    }

    public SELF withBucketName(String bucketName) {
        //because s3cmd transforming bucket name to uppercase
        this.bucketName = bucketName.toUpperCase();
        return self();
    }

    public String getRgwName() {
        return rgwName;
    }

    public SELF withRgwName(String rgwName) {
        this.rgwName = rgwName;
        return self();
    }

    public String getDemoUid() {
        return demoUid;
    }

    public SELF withDemoUid(String demoUid) {
        this.demoUid = demoUid;
        return self();
    }

    public NetworkAutoDetectMode getNetworkAutoDetectMode() {
        return networkAutoDetectMode;
    }

    public SELF withNetworkAutoDetectMode(NetworkAutoDetectMode networkAutoDetectMode) {
        this.networkAutoDetectMode = networkAutoDetectMode;
        return self();
    }

    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public SELF withStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
        return self();
    }

    public enum NetworkAutoDetectMode {
        IPV6_OR_IPV4("1"),
        IPV4_ONLY("4"),
        IPV6_ONLY("6");

        private String value;

        NetworkAutoDetectMode(String value) {
            this.value = value;
        }
    }
}
