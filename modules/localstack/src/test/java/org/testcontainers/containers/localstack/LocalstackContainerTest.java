package org.testcontainers.containers.localstack;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.kms.AWSKMS;
import com.amazonaws.services.kms.AWSKMSClientBuilder;
import com.amazonaws.services.kms.model.CreateKeyRequest;
import com.amazonaws.services.kms.model.CreateKeyResult;
import com.amazonaws.services.kms.model.Tag;
import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogGroupRequest;
import com.amazonaws.services.logs.model.LogGroup;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.CreateQueueResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Tests for Localstack Container, used both in bridge network (exposed to host) and docker network modes.
 * <p>
 * These tests attempt simple interactions with the container to verify behaviour. The bridge network tests use the
 * Java AWS SDK, whereas the docker network tests use an AWS CLI container within the network, to simulate usage of
 * Localstack from within a Docker network.
 */
@Slf4j
@RunWith(Enclosed.class)
public class LocalstackContainerTest {

    public static class WithoutNetwork {

        // without_network {
        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
            .withServices(
                Service.S3,
                Service.SQS,
                Service.CLOUDWATCHLOGS,
                Service.KMS,
                LocalStackContainer.EnabledService.named("events")
            );

        // }

        @Test
        public void s3TestOverBridgeNetwork() throws IOException {
            AmazonS3 s3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                        localstack.getEndpointOverride(Service.S3).toString(),
                        localstack.getRegion()
                    )
                )
                .withCredentials(
                    new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .build();

            final String bucketName = "foo";
            s3.createBucket(bucketName);
            s3.putObject(bucketName, "bar", "baz");

            final List<Bucket> buckets = s3.listBuckets();
            final Optional<Bucket> maybeBucket = buckets
                .stream()
                .filter(b -> b.getName().equals(bucketName))
                .findFirst();
            assertTrue("The created bucket is present", maybeBucket.isPresent());
            final Bucket bucket = maybeBucket.get();

            assertEquals("The created bucket has the right name", bucketName, bucket.getName());

            final ObjectListing objectListing = s3.listObjects(bucketName);
            assertEquals("The created bucket has 1 item in it", 1, objectListing.getObjectSummaries().size());

            final S3Object object = s3.getObject(bucketName, "bar");
            final String content = IOUtils.toString(object.getObjectContent(), Charset.forName("UTF-8"));
            assertEquals("The object can be retrieved", "baz", content);
        }

        @Test
        public void s3TestUsingAwsSdkV2() {
            S3Client s3 = S3Client
                .builder()
                .endpointOverride(localstack.getEndpointOverride(Service.S3))
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            final String bucketName = "foov2";
            s3.createBucket(b -> b.bucket(bucketName));
            assertTrue(
                "New bucket was created",
                s3.listBuckets().buckets().stream().anyMatch(b -> b.name().equals(bucketName))
            );
        }

        @Test
        public void sqsTestOverBridgeNetwork() {
            AmazonSQS sqs = AmazonSQSClientBuilder
                .standard()
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                        localstack.getEndpointOverride(Service.SQS).toString(),
                        localstack.getRegion()
                    )
                )
                .withCredentials(
                    new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .build();

            CreateQueueResult queueResult = sqs.createQueue("baz");
            String fooQueueUrl = queueResult.getQueueUrl();
            assertThat(
                "Created queue has external hostname URL",
                fooQueueUrl,
                containsString(
                    "http://" +
                    DockerClientFactory.instance().dockerHostIpAddress() +
                    ":" +
                    localstack.getMappedPort(LocalStackContainer.PORT)
                )
            );

            sqs.sendMessage(fooQueueUrl, "test");
            final long messageCount = sqs
                .receiveMessage(fooQueueUrl)
                .getMessages()
                .stream()
                .filter(message -> message.getBody().equals("test"))
                .count();
            assertEquals("the sent message can be received", 1L, messageCount);
        }

        @Test
        public void cloudWatchLogsTestOverBridgeNetwork() {
            AWSLogs logs = AWSLogsClientBuilder
                .standard()
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                        localstack.getEndpointOverride(Service.CLOUDWATCHLOGS).toString(),
                        localstack.getRegion()
                    )
                )
                .withCredentials(
                    new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .build();

            logs.createLogGroup(new CreateLogGroupRequest("foo"));

            List<LogGroup> groups = logs.describeLogGroups().getLogGroups();
            assertEquals("One log group should be created", 1, groups.size());
            assertEquals("Name of created log group is [foo]", "foo", groups.get(0).getLogGroupName());
        }

        @Test
        public void kmsKeyCreationTest() {
            AWSKMS awskms = AWSKMSClientBuilder
                .standard()
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                        localstack.getEndpointOverride(Service.KMS).toString(),
                        localstack.getRegion()
                    )
                )
                .withCredentials(
                    new AWSStaticCredentialsProvider(
                        new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .build();

            String desc = String.format("AWS CMK Description");
            Tag createdByTag = new Tag().withTagKey("CreatedBy").withTagValue("StorageService");
            CreateKeyRequest req = new CreateKeyRequest().withDescription(desc).withTags(createdByTag);
            CreateKeyResult key = awskms.createKey(req);

            assertEquals(
                "AWS KMS Customer Managed Key should be created ",
                key.getKeyMetadata().getDescription(),
                desc
            );
        }

        @Test
        public void samePortIsExposedForAllServices() {
            assertTrue("A single port is exposed", localstack.getExposedPorts().size() == 1);
            assertEquals(
                "Endpoint overrides are different",
                localstack.getEndpointOverride(Service.S3).toString(),
                localstack.getEndpointOverride(Service.SQS).toString()
            );
            assertEquals(
                "Endpoint configuration have different endpoints",
                new AwsClientBuilder.EndpointConfiguration(
                    localstack.getEndpointOverride(Service.S3).toString(),
                    localstack.getRegion()
                )
                    .getServiceEndpoint(),
                new AwsClientBuilder.EndpointConfiguration(
                    localstack.getEndpointOverride(Service.SQS).toString(),
                    localstack.getRegion()
                )
                    .getServiceEndpoint()
            );
        }
    }

    public static class WithNetwork {

        // with_network {
        private static Network network = Network.newNetwork();

        @ClassRule
        public static LocalStackContainer localstackInDockerNetwork = new LocalStackContainer(
            LocalstackTestImages.LOCALSTACK_IMAGE
        )
            .withNetwork(network)
            .withNetworkAliases("notthis", "localstack") // the last alias is used for HOSTNAME_EXTERNAL
            .withServices(Service.S3, Service.SQS, Service.CLOUDWATCHLOGS);

        // }

        @ClassRule
        public static GenericContainer<?> awsCliInDockerNetwork = new GenericContainer<>(
            LocalstackTestImages.AWS_CLI_IMAGE
        )
            .withNetwork(network)
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("top"))
            .withEnv("AWS_ACCESS_KEY_ID", "accesskey")
            .withEnv("AWS_SECRET_ACCESS_KEY", "secretkey")
            .withEnv("AWS_REGION", "eu-west-1");

        @Test
        public void s3TestOverDockerNetwork() throws Exception {
            runAwsCliAgainstDockerNetworkContainer("s3api create-bucket --bucket foo");
            runAwsCliAgainstDockerNetworkContainer("s3api list-buckets");
            runAwsCliAgainstDockerNetworkContainer("s3 ls s3://foo");
        }

        @Test
        public void sqsTestOverDockerNetwork() throws Exception {
            final String queueCreationResponse = runAwsCliAgainstDockerNetworkContainer(
                "sqs create-queue --queue-name baz"
            );

            assertThat(
                "Created queue has external hostname URL",
                queueCreationResponse,
                containsString("http://localstack:" + LocalStackContainer.PORT)
            );

            runAwsCliAgainstDockerNetworkContainer(
                String.format(
                    "sqs send-message --endpoint http://localstack:%d --queue-url http://localstack:%d/queue/baz --message-body test",
                    LocalStackContainer.PORT,
                    LocalStackContainer.PORT
                )
            );
            final String message = runAwsCliAgainstDockerNetworkContainer(
                String.format(
                    "sqs receive-message --endpoint http://localstack:%d --queue-url http://localstack:%d/queue/baz",
                    LocalStackContainer.PORT,
                    LocalStackContainer.PORT
                )
            );

            assertTrue("the sent message can be received", message.contains("\"Body\": \"test\""));
        }

        @Test
        public void cloudWatchLogsTestOverDockerNetwork() throws Exception {
            runAwsCliAgainstDockerNetworkContainer("logs create-log-group --log-group-name foo");
        }

        private String runAwsCliAgainstDockerNetworkContainer(String command) throws Exception {
            final String[] commandParts = String
                .format(
                    "/usr/bin/aws --region eu-west-1 %s --endpoint-url http://localstack:%d --no-verify-ssl",
                    command,
                    LocalStackContainer.PORT
                )
                .split(" ");
            final Container.ExecResult execResult = awsCliInDockerNetwork.execInContainer(commandParts);
            Assert.assertEquals(0, execResult.getExitCode());

            final String logs = execResult.getStdout() + execResult.getStderr();
            log.info(logs);
            return logs;
        }
    }

    public static class WithRegion {

        // with_region {
        private static String region = "eu-west-1";

        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
            .withEnv("DEFAULT_REGION", region)
            .withServices(Service.S3);

        // }

        @Test
        public void s3EndpointHasProperRegion() {
            final AwsClientBuilder.EndpointConfiguration endpointConfiguration = new AwsClientBuilder.EndpointConfiguration(
                localstack.getEndpointOverride(Service.S3).toString(),
                localstack.getRegion()
            );
            assertEquals(
                "The endpoint configuration has right region",
                region,
                endpointConfiguration.getSigningRegion()
            );
        }
    }
}
