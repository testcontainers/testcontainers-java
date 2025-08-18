package org.testcontainers.containers.localstack;

import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogGroupsResponse;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.CreateKeyRequest;
import software.amazon.awssdk.services.kms.model.CreateKeyResponse;
import software.amazon.awssdk.services.kms.model.Tag;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.CreateFunctionRequest;
import software.amazon.awssdk.services.lambda.model.CreateFunctionResponse;
import software.amazon.awssdk.services.lambda.model.FunctionCode;
import software.amazon.awssdk.services.lambda.model.GetFunctionConfigurationRequest;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.Runtime;
import software.amazon.awssdk.services.lambda.waiters.LambdaWaiter;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.CreateQueueResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
            .withEnv("SQS_ENDPOINT_STRATEGY", "dynamic")
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
            S3Client s3 = S3Client
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            final String bucketName = "foo";
            s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
            s3.putObject(
                PutObjectRequest.builder().bucket(bucketName).key("bar").build(),
                software.amazon.awssdk.core.sync.RequestBody.fromString("baz")
            );

            final List<Bucket> buckets = s3.listBuckets().buckets();
            final Optional<Bucket> maybeBucket = buckets.stream().filter(b -> b.name().equals(bucketName)).findFirst();
            assertThat(maybeBucket).as("The created bucket is present").isPresent();
            final Bucket bucket = maybeBucket.get();

            assertThat(bucket.name()).as("The created bucket has the right name").isEqualTo(bucketName);

            final ListObjectsV2Response objectListing = s3.listObjectsV2(
                ListObjectsV2Request.builder().bucket(bucketName).build()
            );
            assertThat(objectListing.contents()).as("The created bucket has 1 item in it").hasSize(1);

            final String content = s3
                .getObjectAsBytes(GetObjectRequest.builder().bucket(bucketName).key("bar").build())
                .asString(StandardCharsets.UTF_8);
            assertThat(content).as("The object can be retrieved").isEqualTo("baz");
        }

        @Test
        public void s3TestUsingAwsSdkV2() {
            // with_aws_sdk_v2 {
            S3Client s3 = S3Client
                .builder()
                .endpointOverride(localstack.getEndpoint())
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
            SqsClient sqs = SqsClient
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            CreateQueueResponse queueResult = sqs.createQueue(CreateQueueRequest.builder().queueName("baz").build());
            String fooQueueUrl = queueResult.queueUrl();

            sqs.sendMessage(SendMessageRequest.builder().queueUrl(fooQueueUrl).messageBody("test").build());
            final long messageCount = sqs
                .receiveMessage(ReceiveMessageRequest.builder().queueUrl(fooQueueUrl).build())
                .messages()
                .stream()
                .filter(message -> message.body().equals("test"))
                .count();
            assertThat(messageCount).as("the sent message can be received").isEqualTo(1L);
        }

        @Test
        public void cloudWatchLogsTestOverBridgeNetwork() {
            CloudWatchLogsClient logs = CloudWatchLogsClient
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            logs.createLogGroup(CreateLogGroupRequest.builder().logGroupName("foo").build());

            DescribeLogGroupsResponse response = logs.describeLogGroups();
            assertThat(response.logGroups()).as("One log group should be created").hasSize(1);
            assertThat(response.logGroups().get(0).logGroupName())
                .as("Name of created log group is [foo]")
                .isEqualTo("foo");
        }

        @Test
        public void kmsKeyCreationTest() {
            KmsClient kms = KmsClient
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            String desc = "AWS CMK Description";
            Tag createdByTag = Tag.builder().tagKey("CreatedBy").tagValue("StorageService").build();
            CreateKeyRequest req = CreateKeyRequest.builder().description(desc).tags(createdByTag).build();
            CreateKeyResponse key = kms.createKey(req);

            assertThat(desc)
                .as("AWS KMS Customer Managed Key should be created ")
                .isEqualTo(key.keyMetadata().description());
        }

        @Test
        public void samePortIsExposedForAllServices() {
            assertThat(localstack.getExposedPorts()).as("A single port is exposed").hasSize(1);
            assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                .as("Endpoint overrides are different")
                .isEqualTo(localstack.getEndpointOverride(Service.S3).toString());
        }
    }

    public static class WithNetwork {

        // with_network {
        private static Network network = Network.newNetwork();

        @ClassRule
        public static LocalStackContainer localstackInDockerNetwork = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:0.12.8")
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
        public void localstackHostEnVarIsSet() {
            assertThat(localstackInDockerNetwork.getEnvMap().get("HOSTNAME_EXTERNAL")).isEqualTo("localstack");
        }

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
            assertThat(localstack.getRegion()).as("The endpoint configuration has right region").isEqualTo(region);
        }
    }

    public static class WithoutServices {

        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE);

        @Test
        public void s3ServiceStartLazily() {
            try (
                S3Client s3 = S3Client
                    .builder()
                    .endpointOverride(localstack.getEndpoint())
                    .credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                        )
                    )
                    .region(Region.of(localstack.getRegion()))
                    .build()
            ) {
                assertThat(s3.listBuckets().buckets()).as("S3 Service is started lazily").isEmpty();
            }
        }
    }

    public static class WithVersion2 {

        private static Network network = Network.newNetwork();

        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:2.0")
        )
            .withNetwork(network)
            .withNetworkAliases("localstack");

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
        public void localstackHostEnVarIsSet() {
            assertThat(localstack.getEnvMap().get("LOCALSTACK_HOST")).isEqualTo("localstack");
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

    public static class S3SkipSignatureValidation {

        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
            .withEnv("S3_SKIP_SIGNATURE_VALIDATION", "0");

        @Test
        public void shouldBeAccessibleWithCredentials() throws IOException {
            S3Client s3 = S3Client
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            final String bucketName = "foo";

            s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

            s3.putObject(
                PutObjectRequest.builder().bucket(bucketName).key("bar").build(),
                software.amazon.awssdk.core.sync.RequestBody.fromString("baz")
            );

            final List<Bucket> buckets = s3.listBuckets().buckets();
            final Optional<Bucket> maybeBucket = buckets.stream().filter(b -> b.name().equals(bucketName)).findFirst();
            assertThat(maybeBucket).as("The created bucket is present").isPresent();

            S3Presigner presigner = S3Presigner
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest
                .builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(GetObjectRequest.builder().bucket(bucketName).key("bar").build())
                .build();

            URL presignedUrl = presigner.presignGetObject(presignRequest).url();

            assertThat(presignedUrl).as("The presigned url is valid").isNotNull();
            final String content = IOUtils.toString(presignedUrl, StandardCharsets.UTF_8);
            assertThat(content).as("The object can be retrieved").isEqualTo("baz");
        }
    }

    public static class LambdaContainerLabels {

        @ClassRule
        public static LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE);

        private static byte[] createLambdaHandlerZipFile() throws IOException {
            StringBuilder sb = new StringBuilder();
            sb.append("def handler(event, context):\n");
            sb.append("    return event");

            ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
            ZipOutputStream out = new ZipOutputStream(byteOutput);
            ZipEntry e = new ZipEntry("handler.py");
            out.putNextEntry(e);

            byte[] data = sb.toString().getBytes();
            out.write(data, 0, data.length);
            out.closeEntry();
            out.close();
            return byteOutput.toByteArray();
        }

        @Test
        public void shouldLabelLambdaContainers() throws IOException {
            LambdaClient lambda = LambdaClient
                .builder()
                .endpointOverride(localstack.getEndpoint())
                .credentialsProvider(
                    StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                    )
                )
                .region(Region.of(localstack.getRegion()))
                .build();

            // create function
            byte[] handlerFile = createLambdaHandlerZipFile();
            CreateFunctionRequest createFunctionRequest = CreateFunctionRequest
                .builder()
                .functionName("test-function")
                .runtime(Runtime.PYTHON3_11)
                .handler("handler.handler")
                .role("arn:aws:iam::000000000000:role/test-role")
                .code(FunctionCode.builder().zipFile(SdkBytes.fromByteArray(handlerFile)).build())
                .build();
            CreateFunctionResponse createFunctionResult = lambda.createFunction(createFunctionRequest);

            try (LambdaWaiter waiter = lambda.waiter()) {
                waiter.waitUntilFunctionActive(
                    GetFunctionConfigurationRequest.builder().functionName(createFunctionResult.functionName()).build()
                );
            }

            // invoke function once
            String payload = "{\"test\": \"payload\"}";
            InvokeRequest invokeRequest = InvokeRequest
                .builder()
                .functionName(createFunctionResult.functionName())
                .payload(SdkBytes.fromUtf8String(payload))
                .build();
            InvokeResponse invokeResult = lambda.invoke(invokeRequest);
            assertThat(invokeResult.payload().asUtf8String())
                .as("Invoke result not matching expected output")
                .isEqualTo(payload);

            // assert that the spawned lambda containers has the testcontainers labels set
            DockerClient dockerClient = DockerClientFactory.instance().client();
            Collection<String> nameFilter = Collections.singleton(localstack.getContainerName().replace("_", "-"));
            com.github.dockerjava.api.model.Container lambdaContainer = dockerClient
                .listContainersCmd()
                .withNameFilter(nameFilter)
                .exec()
                .stream()
                .findFirst()
                .orElse(null);
            assertThat(lambdaContainer).as("Lambda container not found").isNotNull();
            Map<String, String> labels = lambdaContainer.getLabels();
            assertThat(labels.get("org.testcontainers")).as("TestContainers label not present").isEqualTo("true");
            assertThat(labels.get("org.testcontainers.sessionId"))
                .as("TestContainers session id not present")
                .isNotNull();
        }
    }
}
