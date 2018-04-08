package org.testcontainers.containers.localstack;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

public class SimpleLocalstackS3Test {

    @Rule
    public LocalStackContainer localstack = new LocalStackContainer()
            .withServices(S3);

    @Test
    public void s3Test() throws IOException {
        AmazonS3 s3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();

        s3.createBucket("foo");
        s3.putObject("foo", "bar", "baz");

        final List<Bucket> buckets = s3.listBuckets();
        assertEquals("The created bucket is present", 1, buckets.size());
        final Bucket bucket = buckets.get(0);

        assertEquals("The created bucket has the right name", "foo", bucket.getName());
        assertEquals("The created bucket has the right name", "foo", bucket.getName());

        final ObjectListing objectListing = s3.listObjects("foo");
        assertEquals("The created bucket has 1 item in it", 1, objectListing.getObjectSummaries().size());

        final S3Object object = s3.getObject("foo", "bar");
        final String content = IOUtils.toString(object.getObjectContent(), Charset.forName("UTF-8"));
        assertEquals("The object can be retrieved", "baz", content);

    }
}
