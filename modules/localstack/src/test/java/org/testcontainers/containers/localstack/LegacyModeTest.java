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
                        "0.11 with legacy = off",
                        new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_11_IMAGE, false),
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

    @RunWith(Parameterized.class)
    @AllArgsConstructor
    public static class LegacyModeUnitTest {

        private final String version;

        private final boolean shouldUseLegacyMode;

        @Parameterized.Parameters(name = "{0} - {1}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(
                new Object[][] {
                    { "latest", false },
                    { "s3-latest", false },
                    { "latest-bigdata", false },
                    { "3.4.0-bigdata", false },
                    { "3.4.0@sha256:54fcf172f6ff70909e1e26652c3bb4587282890aff0d02c20aa7695469476ac0", false },
                    { "1.4@sha256:7badf31c550f81151c485980e17542592942d7f05acc09723c5f276d41b5927d", false },
                    { "3.4.0", false },
                    { "0.12", false },
                    { "0.11", false },
                    { "sha256:8bf0d744fea26603f2b11ef7206edb38375ef954258afaeda96532a6c9c1ab8b", false },
                    { "0.10.7@sha256:45ef287e29af7285c6e4013fafea1e3567c167cd22d12282f0a5f9c7894b1c5f", true },
                    { "0.10.7", true },
                    { "0.9.6", true },
                }
            );
        }

        @Test
        public void samePortIsExposedForAllServices() {
            assertThat(LocalStackContainer.shouldRunInLegacyMode(version)).isEqualTo(shouldUseLegacyMode);
        }
    }
}
