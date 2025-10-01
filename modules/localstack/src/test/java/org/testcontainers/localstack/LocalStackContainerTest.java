package org.testcontainers.localstack;

import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.localstack.LocalstackTestImages;
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

@Slf4j
class LocalStackContainerTest {

    @Nested
    class WithoutNetwork {

        @Test
        void s3TestOverBridgeNetwork() {
            try (
                // container {
                LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
                    .withServices("s3")
                // }
            ) {
                localstack.start();

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

                final String bucketName = "foo";
                s3.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                s3.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key("bar").build(),
                    software.amazon.awssdk.core.sync.RequestBody.fromString("baz")
                );

                final List<Bucket> buckets = s3.listBuckets().buckets();
                final Optional<Bucket> maybeBucket = buckets
                    .stream()
                    .filter(b -> b.name().equals(bucketName))
                    .findFirst();
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
        }

        @Test
        void sqsTestOverBridgeNetwork() {
            try (
                LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
                    .withEnv("SQS_ENDPOINT_STRATEGY", "dynamic")
                    .withServices("sqs")
            ) {
                localstack.start();

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

                CreateQueueResponse queueResult = sqs.createQueue(
                    CreateQueueRequest.builder().queueName("baz").build()
                );
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
        }

        @Test
        void cloudWatchLogsTestOverBridgeNetwork() {
            try (
                LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
                    .withServices("logs")
            ) {
                localstack.start();

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
        }

        @Test
        void kmsKeyCreationTest() {
            try (
                LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
                    .withServices("kms")
            ) {
                localstack.start();
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
        }

        @Test
        void samePortIsExposedForAllServices() {
            try (LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)) {
                localstack.start();

                assertThat(localstack.getExposedPorts()).as("A single port is exposed").hasSize(1);
                assertThat(localstack.getEndpoint().toString())
                    .as("Endpoint overrides are different")
                    .isEqualTo(localstack.getEndpoint().toString());
            }
        }
    }

    @Nested
    class WithNetwork {

        // with_network {
        Network network = Network.newNetwork();

        LocalStackContainer localstackInDockerNetwork = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
            .withNetwork(network)
            .withNetworkAliases("localstack")
            .withServices("s3", "sqs", "logs");
        // }

        GenericContainer<?> awsCliInDockerNetwork = new GenericContainer<>(LocalstackTestImages.AWS_CLI_IMAGE)
            .withNetwork(network)
            .withCreateContainerCmdModifier(cmd -> cmd.withEntrypoint("tail"))
            .withCommand(" -f /dev/null")
            .withEnv("AWS_ACCESS_KEY_ID", "accesskey")
            .withEnv("AWS_SECRET_ACCESS_KEY", "secretkey")
            .withEnv("AWS_REGION", "eu-west-1");

        @BeforeEach
        void setup() {
            localstackInDockerNetwork.start();
            awsCliInDockerNetwork.start();
        }

        @AfterEach
        void tearDown() {
            awsCliInDockerNetwork.stop();
            localstackInDockerNetwork.stop();
        }

        @Test
        void s3TestOverDockerNetwork() throws Exception {
            runAwsCliAgainstDockerNetworkContainer(
                "s3api create-bucket --bucket foo --create-bucket-configuration LocationConstraint=eu-west-1"
            );
            runAwsCliAgainstDockerNetworkContainer("s3api list-buckets");
            runAwsCliAgainstDockerNetworkContainer("s3 ls s3://foo");
        }

        @Test
        void sqsTestOverDockerNetwork() throws Exception {
            final String queueCreationResponse = runAwsCliAgainstDockerNetworkContainer(
                "sqs create-queue --queue-name baz"
            );

            runAwsCliAgainstDockerNetworkContainer(
                String.format(
                    "sqs send-message --endpoint http://localstack:%d --queue-url http://sqs.eu-west-1.localhost.localstack.cloud:%d/000000000000/baz --message-body test",
                    LocalStackContainer.PORT,
                    LocalStackContainer.PORT
                )
            );
            final String message = runAwsCliAgainstDockerNetworkContainer(
                String.format(
                    "sqs receive-message --endpoint http://localstack:%d --queue-url http://sqs.eu-west-1.localhost.localstack.cloud:%d/000000000000/baz",
                    LocalStackContainer.PORT,
                    LocalStackContainer.PORT
                )
            );

            assertThat(message).as("the sent message can be received").contains("\"Body\": \"test\"");
        }

        @Test
        void cloudWatchLogsTestOverDockerNetwork() throws Exception {
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

    @Nested
    class WithRegion {

        @Test
        void s3EndpointHasProperRegion() {
            try (
                // with_region {
                LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
                    .withEnv("DEFAULT_REGION", "eu-west-1")
                    .withServices("s3");
                // }
            ) {
                localstack.start();
                assertThat(localstack.getRegion())
                    .as("The endpoint configuration has right region")
                    .isEqualTo("eu-west-1");
            }
        }
    }

    @Nested
    class WithoutServices {

        @Test
        void s3ServiceStartLazily() {
            try (LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE);) {
                localstack.start();

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
                assertThat(s3.listBuckets().buckets()).as("S3 Service is started lazily").isEmpty();
            }
        }
    }

    @Nested
    class S3SkipSignatureValidation {

        @Test
        void shouldBeAccessibleWithCredentials() throws IOException {
            try (
                LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)
                    .withEnv("S3_SKIP_SIGNATURE_VALIDATION", "0")
            ) {
                localstack.start();

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
                final Optional<Bucket> maybeBucket = buckets
                    .stream()
                    .filter(b -> b.name().equals(bucketName))
                    .findFirst();
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
    }

    @Nested
    class LambdaContainerLabels {

        private byte[] createLambdaHandlerZipFile() throws IOException {
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
        void shouldLabelLambdaContainers() throws IOException {
            try (LocalStackContainer localstack = new LocalStackContainer(LocalstackTestImages.LOCALSTACK_IMAGE)) {
                localstack.start();

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
                        GetFunctionConfigurationRequest
                            .builder()
                            .functionName(createFunctionResult.functionName())
                            .build()
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
}
