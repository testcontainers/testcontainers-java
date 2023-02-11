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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
            // with_aws_sdk_v1 {
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
            // }

            final String bucketName = "foo";
            s3.createBucket(bucketName);
            s3.putObject(bucketName, "bar", "baz");

            final List<Bucket> buckets = s3.listBuckets();
            final Optional<Bucket> maybeBucket = buckets
                .stream()
                .filter(b -> b.getName().equals(bucketName))
                .findFirst();
            assertThat(maybeBucket).as("The created bucket is present").isPresent();
            final Bucket bucket = maybeBucket.get();

            assertThat(bucket.getName()).as("The created bucket has the right name").isEqualTo(bucketName);

            final ObjectListing objectListing = s3.listObjects(bucketName);
            assertThat(objectListing.getObjectSummaries()).as("The created bucket has 1 item in it").hasSize(1);

            final S3Object object = s3.getObject(bucketName, "bar");
            final String content = IOUtils.toString(object.getObjectContent(), StandardCharsets.UTF_8);
            assertThat(content).as("The object can be retrieved").isEqualTo("baz");
        }

        @Test
        public void s3TestUsingAwsSdkV2() {
            // with_aws_sdk_v2 {
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
            // }

            final String bucketName = "foov2";
            s3.createBucket(b -> b.bucket(bucketName));
            assertThat(s3.listBuckets().buckets().stream().anyMatch(b -> b.name().equals(bucketName)))
                .as("New bucket was created")
                .isTrue();
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
            assertThat(fooQueueUrl)
                .as("Created queue has external hostname URL")
                .contains("http://" + localstack.getHost() + ":" + localstack.getMappedPort(LocalStackContainer.PORT));

            sqs.sendMessage(fooQueueUrl, "test");
            final long messageCount = sqs
                .receiveMessage(fooQueueUrl)
                .getMessages()
                .stream()
                .filter(message -> message.getBody().equals("test"))
                .count();
            assertThat(messageCount).as("the sent message can be received").isEqualTo(1L);
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
            assertThat(groups).as("One log group should be created").hasSize(1);
            assertThat(groups.get(0).getLogGroupName()).as("Name of created log group is [foo]").isEqualTo("foo");
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

            assertThat(desc)
                .as("AWS KMS Customer Managed Key should be created ")
                .isEqualTo(key.getKeyMetadata().getDescription());
        }

        @Test
        public void samePortIsExposedForAllServices() {
            assertThat(localstack.getExposedPorts()).as("A single port is exposed").hasSize(1);
            assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                .as("Endpoint overrides are different")
                .isEqualTo(localstack.getEndpointOverride(Service.S3).toString());
            assertThat(
                new AwsClientBuilder.EndpointConfiguration(
                    localstack.getEndpointOverride(Service.SQS).toString(),
                    localstack.getRegion()
                )
                    .getServiceEndpoint()
            )
                .as("Endpoint configuration have different endpoints")
                .isEqualTo(
                    new AwsClientBuilder.EndpointConfiguration(
                        localstack.getEndpointOverride(Service.S3).toString(),
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
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("tail"))
            .withCommand(" -f /dev/null")
            .withEnv("AWS_ACCESS_KEY_ID", "accesskey")
            .withEnv("AWS_SECRET_ACCESS_KEY", "secretkey")
            .withEnv("AWS_REGION", "eu-west-1");

        @Test
        public void s3TestOverDockerNetwork() throws Exception {
            runAwsCliAgainstDockerNetworkContainer(
                "s3api create-bucket --bucket foo --create-bucket-configuration LocationConstraint=eu-west-1"
            );
            runAwsCliAgainstDockerNetworkContainer("s3api list-buckets");
            runAwsCliAgainstDockerNetworkContainer("s3 ls s3://foo");
        }

        @Test
        public void sqsTestOverDockerNetwork() throws Exception {
            final String queueCreationResponse = runAwsCliAgainstDockerNetworkContainer(
                "sqs create-queue --queue-name baz"
            );

            assertThat(queueCreationResponse)
                .as("Created queue has external hostname URL")
                .contains("http://localstack:" + LocalStackContainer.PORT);

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

            assertThat(message).as("the sent message can be received").contains("\"Body\": \"test\"");
        }

        @Test
        public void cloudWatchLogsTestOverDockerNetwork() throws Exception {
            runAwsCliAgainstDockerNetworkContainer("logs create-log-group --log-group-name foo");
        }

        private String runAwsCliAgainstDockerNetworkContainer(String command) throws Exception {
            final String[] commandParts = String
                .format(
                    "/usr/local/bin/aws --region eu-west-1 %s --endpoint-url http://localstack:%d --no-verify-ssl",
                    command,
                    LocalStackContainer.PORT
                )
                .split(" ");
            final Container.ExecResult execResult = awsCliInDockerNetwork.execInContainer(commandParts);
            assertThat(execResult.getExitCode()).isEqualTo(0);

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
            assertThat(endpointConfiguration.getSigningRegion())
                .as("The endpoint configuration has right region")
                .isEqualTo(region);
        }
    }

    public static class WithoutServices {

        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(
            LocalstackTestImages.LOCALSTACK_0_13_IMAGE
        );

        @Test
        public void s3ServiceStartLazily() {
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
            assertThat(s3.listBuckets().buckets()).as("S3 Service is started lazily").isEmpty();
        }
    }
}
