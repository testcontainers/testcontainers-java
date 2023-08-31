package org.testcontainers.containers;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.ObjectWriteResponse;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.UploadObjectArgs;
import org.junit.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

public class MinIOContainerTest {

    @Test
    public void testBasicUsage() throws Exception {
        MinIOContainer container = new MinIOContainer("minio/minio:latest");
        container.start();

        MinioClient minioClient = MinioClient
            .builder()
            .endpoint(container.getS3URL())
            .credentials(container.getUserName(), container.getPassword())
            .build();

        minioClient.makeBucket(MakeBucketArgs.builder().bucket("test-bucket").region("us-west-2").build());

        BucketExistsArgs existsArgs = BucketExistsArgs.builder().bucket("test-bucket").build();

        assertThat(minioClient.bucketExists(existsArgs)).isTrue();

        URL file = this.getClass().getResource("/object_to_upload.txt");
        assertThat(file).isNotNull();
        ObjectWriteResponse upload = minioClient.uploadObject(
            UploadObjectArgs.builder().bucket("test-bucket").object("my-objectname").filename(file.getPath()).build()
        );

        StatObjectResponse objectStat = minioClient.statObject(
            StatObjectArgs.builder().bucket("test-bucket").object("my-objectname").build()
        );

        assertThat(objectStat.object()).isEqualTo("my-objectname");
    }
}
