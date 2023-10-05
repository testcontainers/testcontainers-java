package org.testcontainers.containers;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CephContainerTest {

    @Test
    public void testBasicUsage() throws Exception {
        try (
            // minioContainer {
            CephContainer container = new CephContainer("quay.io/ceph/demo:latest");
            // }
        ) {
            container.start();

            // configuringClient {
            AWSCredentials credentials = new BasicAWSCredentials(
                container.getCephAccessKey(),
                container.getCephSecretKey()
            );
            AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                container.getCephUrl().toString(),
                ""
            );
            AmazonS3 s3client = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withPathStyleAccessEnabled(true)
                .build();
            // }

            s3client.createBucket("test-bucket");
            assertThat(s3client.doesBucketExistV2("test-bucket"));

            URL file = this.getClass().getResource("/object_to_upload.txt");
            assertThat(file).isNotNull();
            s3client.putObject("test-bucket", "my-objectname", file.getFile());

            List<S3ObjectSummary> objets = s3client.listObjectsV2("test-bucket").getObjectSummaries();
            assertThat(objets.size()).isEqualTo(1);
            assertThat(objets.get(0).getKey()).isEqualTo("my-objectname");
        }
    }

    @Test
    public void testDefaultUserPassword() {
        try (CephContainer container = new CephContainer("quay.io/ceph/demo:latest")) {
            container.start();
            assertThat(container.getCephAccessKey()).isNotBlank();
            assertThat(container.getCephSecretKey()).isNotBlank();
        }
    }

    @Test
    public void testOverwriteUserPassword() {
        try (
            // cephOverrides {
            CephContainer container = new CephContainer("quay.io/ceph/demo:latest")
                .withCephAccessKey("testuser123")
                .withCephSecretKey("testpassword123");
            // }
        ) {
            container.start();
            assertThat(container.getCephAccessKey()).isEqualTo("testuser123");
            assertThat(container.getCephSecretKey()).isEqualTo("testpassword123");
        }
    }
}
