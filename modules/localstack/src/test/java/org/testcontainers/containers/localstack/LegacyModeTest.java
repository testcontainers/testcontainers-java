package org.testcontainers.containers.localstack;

import com.github.dockerjava.api.DockerClient;
import lombok.AllArgsConstructor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

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
            localstack.withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS);
            localstack.start();

            try {
                assertTrue("A single port is exposed", localstack.getExposedPorts().size() == 1);
                assertEquals(
                    "Endpoint overrides are different",
                    localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                    localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString()
                );
                assertEquals(
                    "Endpoint configuration have different endpoints",
                    localstack.getEndpointConfiguration(LocalStackContainer.Service.S3).getServiceEndpoint(),
                    localstack.getEndpointConfiguration(LocalStackContainer.Service.SQS).getServiceEndpoint()
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
            localstack.withServices(LocalStackContainer.Service.S3, LocalStackContainer.Service.SQS);
            localstack.start();

            try {
                assertTrue("Multiple ports are exposed", localstack.getExposedPorts().size() > 1);
                assertNotEquals(
                    "Endpoint overrides are different",
                    localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                    localstack.getEndpointOverride(LocalStackContainer.Service.SQS).toString()
                );
                assertNotEquals(
                    "Endpoint configuration have different endpoints",
                    localstack.getEndpointConfiguration(LocalStackContainer.Service.S3).getServiceEndpoint(),
                    localstack.getEndpointConfiguration(LocalStackContainer.Service.SQS).getServiceEndpoint()
                );
            } finally {
                localstack.stop();
            }
        }
    }
}
