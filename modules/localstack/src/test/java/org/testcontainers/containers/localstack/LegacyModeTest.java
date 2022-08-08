package org.testcontainers.containers.localstack;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.github.dockerjava.api.DockerClient;
import lombok.AllArgsConstructor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Enclosed.class)
public class LegacyModeTest {

    private static DockerImageName LOCALSTACK_CUSTOM_TAG = LocalstackTestImages.LOCALSTACK_IMAGE.withTag("custom");

    @RunWith(Parameterized.class)
    @AllArgsConstructor
    public static class Off {

        private final String description;

        private final LocalStackContainer localstack;

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(
                new Object[][] {
                    { "0.12", new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_12_IMAGE) },
                    { "0.11", new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_11_IMAGE) },
                    {
                        "0.7 with legacy = off",
                        new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_7_IMAGE, false),
                    },
                }
            );
        }

        @Test
        public void samePortIsExposedForAllServices() {
            localstack.withServices(Service.S3, Service.SQS);
            localstack.start();

            try {
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
            } finally {
                localstack.stop();
            }
        }
    }

    @RunWith(Parameterized.class)
    @AllArgsConstructor
    public static class On {

        private final String description;

        private final LocalStackContainer localstack;

        @BeforeClass
        public static void createCustomTag() {
            DockerClient dockerClient = DockerClientFactory.instance().client();
            dockerClient
                .tagImageCmd(
                    new RemoteDockerImage(LocalstackTestImages.LOCALSTACK_0_12_IMAGE).get(),
                    LOCALSTACK_CUSTOM_TAG.getRepository(),
                    LOCALSTACK_CUSTOM_TAG.getVersionPart()
                )
                .exec();
        }

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(
                new Object[][] {
                    { "0.10", new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_10_IMAGE) },
                    { "custom", new LocalStackContainer(LOCALSTACK_CUSTOM_TAG) },
                    {
                        "0.11 with legacy = on",
                        new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_11_IMAGE, true),
                    },
                }
            );
        }

        @Test
        public void differentPortsAreExposed() {
            localstack.withServices(Service.S3, Service.SQS);
            localstack.start();

            try {
                assertThat(localstack.getExposedPorts()).as("Multiple ports are exposed").hasSizeGreaterThan(1);
                assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                    .as("Endpoint overrides are different")
                    .isNotEqualTo(localstack.getEndpointOverride(Service.S3).toString());
                assertThat(
                    new AwsClientBuilder.EndpointConfiguration(
                        localstack.getEndpointOverride(Service.SQS).toString(),
                        localstack.getRegion()
                    )
                        .getServiceEndpoint()
                )
                    .as("Endpoint configuration have different endpoints")
                    .isNotEqualTo(
                        new AwsClientBuilder.EndpointConfiguration(
                            localstack.getEndpointOverride(Service.S3).toString(),
                            localstack.getRegion()
                        )
                            .getServiceEndpoint()
                    );
            } finally {
                localstack.stop();
            }
        }
    }
}
