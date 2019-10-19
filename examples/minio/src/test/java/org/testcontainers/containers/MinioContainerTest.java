package com.testcontainers.minio;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.Bucket;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class MinioContainerTest {

    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_KEY = "secretKey";
    private static final String BUCKET = "bucket";

    private AmazonS3Client client = null;

    @After
    public void shutDown() {
        if (client != null) {
            client.shutdown();
            client = null;
        }
    }

    @Test
    public void testCreateBucket() {
        try (MinioContainer container = new MinioContainer(
            new MinioContainer.CredentialsProvider(ACCESS_KEY, SECRET_KEY))) {

            container.start();
            client = getClient(container);
            Bucket bucket = client.createBucket(BUCKET);
            assertNotNull(bucket);
            assertEquals(BUCKET, bucket.getName());

            List<Bucket> buckets = client.listBuckets();
            assertNotNull(buckets);
            assertEquals(1, buckets.size());
            assertTrue(buckets.stream()
                .map(Bucket::getName)
                .collect(Collectors.toList())
                .contains(BUCKET));
        }
    }

    private AmazonS3Client getClient(MinioContainer container) {
        S3ClientOptions clientOptions = S3ClientOptions
            .builder()
            .setPathStyleAccess(true)
            .build();

        client = new AmazonS3Client(new AWSStaticCredentialsProvider(
            new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY)));
        client.setEndpoint("http://" + container.getHostAddress());
        client.setS3ClientOptions(clientOptions);
        return client;
    }
}
