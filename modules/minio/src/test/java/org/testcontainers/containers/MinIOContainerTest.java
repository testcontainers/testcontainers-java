package org.testcontainers.containers;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import io.minio.messages.Bucket;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MinIOContainerTest {

    static final String MINIO_DOCKER_IMAGE_NAME = "minio/minio:RELEASE.2023-09-04T19-57-37Z";

    @Test
    void testBasicUsage() throws Exception {
        try (
            // minioContainer {
            MinIOContainer container = new MinIOContainer(MINIO_DOCKER_IMAGE_NAME);
            // }
        ) {
            container.start();

            // configuringClient {
            MinioClient minioClient = MinioClient
                .builder()
                .endpoint(container.getS3URL())
                .credentials(container.getUserName(), container.getPassword())
                .build();

            // }
            minioClient.makeBucket(MakeBucketArgs.builder().bucket("test-bucket").region("us-west-2").build());

            BucketExistsArgs existsArgs = BucketExistsArgs.builder().bucket("test-bucket").build();

            assertThat(minioClient.bucketExists(existsArgs)).isTrue();

            URL file = this.getClass().getResource("/object_to_upload.txt");
            assertThat(file).isNotNull();
            minioClient.uploadObject(
                UploadObjectArgs
                    .builder()
                    .bucket("test-bucket")
                    .object("my-objectname")
                    .filename(file.getPath())
                    .build()
            );

            StatObjectResponse objectStat = minioClient.statObject(
                StatObjectArgs.builder().bucket("test-bucket").object("my-objectname").build()
            );

            assertThat(objectStat.object()).isEqualTo("my-objectname");
        }
    }

    @Test
    void testDefaultUserPassword() {
        try (MinIOContainer container = new MinIOContainer(MINIO_DOCKER_IMAGE_NAME)) {
            container.start();
            assertThat(container.getUserName()).isEqualTo("minioadmin");
            assertThat(container.getPassword()).isEqualTo("minioadmin");
        }
    }

    @Test
    void testOverwriteUserPassword() {
        try (
            // minioOverrides {
            MinIOContainer container = new MinIOContainer(MINIO_DOCKER_IMAGE_NAME)
                .withUserName("testuser")
                .withPassword("testpassword");
            // }
        ) {
            container.start();
            assertThat(container.getUserName()).isEqualTo("testuser");
            assertThat(container.getPassword()).isEqualTo("testpassword");
        }
    }

    @Test
    void testWithBuckets() throws Exception {
        try (MinIOContainer container = new MinIOContainer(MINIO_DOCKER_IMAGE_NAME)
            .withUserName("testuser")
            .withPassword("testpassword")
            .withBuckets("first", "second")) {
            container.start();

            MinioClient minioClient = MinioClient
                .builder()
                .endpoint(container.getS3URL())
                .credentials(container.getUserName(), container.getPassword())
                .build();

            BucketExistsArgs firstBucketArgs = BucketExistsArgs.builder().bucket("first").build();
            BucketExistsArgs secondBucketArgs = BucketExistsArgs.builder().bucket("second").build();

            List<Bucket> bucketList = minioClient.listBuckets();

            assertThat(bucketList).hasSize(2);
            assertThat(minioClient.bucketExists(firstBucketArgs)).isTrue();
            assertThat(minioClient.bucketExists(secondBucketArgs)).isTrue();

            minioClient.close();
        }
    }

    @Test
    void testWithBucketsEmpty() throws Exception {
        try (MinIOContainer container = new MinIOContainer(MINIO_DOCKER_IMAGE_NAME)
            .withUserName("testuser")
            .withPassword("testpassword")
            .withBuckets()) {
            container.start();

            MinioClient minioClient = MinioClient
                .builder()
                .endpoint(container.getS3URL())
                .credentials(container.getUserName(), container.getPassword())
                .build();

            assertThat(minioClient.listBuckets()).isEmpty();

            minioClient.close();
        }
    }

}
