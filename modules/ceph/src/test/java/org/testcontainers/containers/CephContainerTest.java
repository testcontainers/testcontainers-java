package org.testcontainers.containers;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CephContainerTest {

    public CephContainer cephContainer;

    private AmazonS3 amazonS3;

    @Before
    public void setUp() {
        // creating_container {
        cephContainer = new CephContainer("v3.2.13-stable-3.2-mimic-centos-7")
            .withAwsAccessKey("test")
            .withAwsSecretKey("test")
            .withBucketName("test");
        // }

        // setting_up_s3_client {
        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(cephContainer.getAWSCredentialsProvider())
                .withEndpointConfiguration(cephContainer.getAWSEndpointConfiguration())
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(
                        new ClientConfiguration()
                                .withProtocol(Protocol.HTTP)
                                .withSignerOverride("S3SignerType")
                )
                .build();
        // }
    }

    @Test
    public void testS3Bucket() {
        String bucketName = "test2";

        //check current bucket
        assertTrue(amazonS3.doesBucketExistV2(cephContainer.getBucketName()));

        //create another bucket
        amazonS3.createBucket(bucketName);
        assertTrue(amazonS3.doesBucketExistV2(bucketName));
        List<Bucket> buckets = amazonS3.listBuckets();
        assertEquals(2, buckets.size());
        assertTrue(buckets.stream().anyMatch(
                bucket -> bucket.getName().equals(bucketName)
        ));

        //remove bucket
        amazonS3.deleteBucket(bucketName);
        assertFalse(amazonS3.doesBucketExistV2(bucketName));
        buckets = amazonS3.listBuckets();
        assertEquals(1, buckets.size());
        assertFalse(buckets.stream().anyMatch(
                bucket -> bucket.getName().equals(bucketName)
        ));
    }

    @Test
    public void testS3Object() throws IOException {
        String objectId = "test";
        String testData = "This is test data";

        //put object
        amazonS3.putObject(cephContainer.getBucketName(), objectId, testData);
        assertEquals(1, amazonS3.listObjects(cephContainer.getBucketName()).getObjectSummaries().size());
        S3Object object = amazonS3.getObject(cephContainer.getBucketName(), objectId);
        assertEquals(testData, IOUtils.toString(object.getObjectContent(), StandardCharsets.UTF_8));

        //generate presigned url and download file
        URL url = amazonS3.generatePresignedUrl(
                cephContainer.getBucketName(),
                objectId,
                Date.from(Instant.now().plusSeconds(Duration.ofMinutes(5).toMillis())));

        try (InputStream inputStream = url.openStream()) {
            assertEquals(testData, IOUtils.toString(inputStream, StandardCharsets.UTF_8));
        }
    }
}
