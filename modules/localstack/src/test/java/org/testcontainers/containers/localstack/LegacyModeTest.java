package org.testcontainers.containers.localstack;

import com.github.dockerjava.api.DockerClient;
import lombok.AllArgsConstructor;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.SQS;
import static org.testcontainers.containers.localstack.LocalstackTestImages.LOCALSTACK_0_10_IMAGE;
import static org.testcontainers.containers.localstack.LocalstackTestImages.LOCALSTACK_0_11_IMAGE;
import static org.testcontainers.containers.localstack.LocalstackTestImages.LOCALSTACK_0_12_IMAGE;
import static org.testcontainers.containers.localstack.LocalstackTestImages.LOCALSTACK_0_7_IMAGE;
import static org.testcontainers.containers.localstack.LocalstackTestImages.LOCALSTACK_IMAGE;

@RunWith(Enclosed.class)
public class LegacyModeTest {
    private static DockerImageName LOCALSTACK_CUSTOM_TAG = LOCALSTACK_IMAGE.withTag("custom");

    @RunWith(Parameterized.class)
    @AllArgsConstructor
    public static class Off {
        private final String description;
        private final LocalStackContainer localstack;

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(new Object[][]{
                {"0.12", new LocalStackContainer(LOCALSTACK_0_12_IMAGE)},
                {"0.11", new LocalStackContainer(LOCALSTACK_0_11_IMAGE)},
                {"0.7 with legacy = off", new LocalStackContainer(LOCALSTACK_0_7_IMAGE, false)}
            });
        }

        @Test
        public void samePortIsExposedForAllServices() {
            localstack.withServices(S3, SQS);
            localstack.start();

            try {
                assertTrue("A single port is exposed", localstack.getExposedPorts().size() == 1);
                assertEquals(
                    "Endpoint overrides are different",
                    localstack.getEndpointOverride(S3).toString(),
                    localstack.getEndpointOverride(SQS).toString());
                assertEquals(
                    "Endpoint configuration have different endpoints",
                    localstack.getEndpointConfiguration(S3).getServiceEndpoint(),
                    localstack.getEndpointConfiguration(SQS).getServiceEndpoint());
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
            DockerClientFactory
                .instance()
                .checkAndPullImage(
                    dockerClient,
                    LOCALSTACK_0_12_IMAGE.asCanonicalNameString()
                );
            dockerClient
                .tagImageCmd(
                    LOCALSTACK_0_12_IMAGE.asCanonicalNameString(),
                    LOCALSTACK_CUSTOM_TAG.getRepository(),
                    LOCALSTACK_CUSTOM_TAG.getVersionPart()
                )
                .exec();
        }

        @Parameterized.Parameters(name = "{0}")
        public static Iterable<Object[]> constructors() {
            return Arrays.asList(new Object[][]{
                {"0.10", new LocalStackContainer(LOCALSTACK_0_10_IMAGE)},
                {"custom", new LocalStackContainer(LOCALSTACK_CUSTOM_TAG)},
                {"0.11 with legacy = on", new LocalStackContainer(LOCALSTACK_0_11_IMAGE, true)}
            });
        }

        @Test
        public void differentPortsAreExposed() {
            localstack.withServices(S3, SQS);
            localstack.start();

            try {
                assertTrue("Multiple ports are exposed", localstack.getExposedPorts().size() > 1);
                assertNotEquals(
                    "Endpoint overrides are different",
                    localstack.getEndpointOverride(S3).toString(),
                    localstack.getEndpointOverride(SQS).toString());
                assertNotEquals(
                    "Endpoint configuration have different endpoints",
                    localstack.getEndpointConfiguration(S3).getServiceEndpoint(),
                    localstack.getEndpointConfiguration(SQS).getServiceEndpoint());
            } finally {
                localstack.stop();
            }
        }
    }
}
