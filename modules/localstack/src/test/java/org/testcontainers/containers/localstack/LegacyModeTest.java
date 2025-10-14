package org.testcontainers.containers.localstack;

import com.github.dockerjava.api.DockerClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.images.RemoteDockerImage;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyModeTest {

    private static final DockerImageName LOCALSTACK_CUSTOM_TAG = DockerImageName
        .parse("localstack/localstack:0.12.8")
        .withTag("custom");

    @BeforeAll
    static void setup() {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        dockerClient
            .tagImageCmd(
                new RemoteDockerImage(LocalstackTestImages.LOCALSTACK_0_12_IMAGE).get(),
                LOCALSTACK_CUSTOM_TAG.getRepository(),
                LOCALSTACK_CUSTOM_TAG.getVersionPart()
            )
            .exec();
    }

    static Stream<Arguments> localstackVersionWithLegacyOff() {
        return Stream.of(
            Arguments.arguments("0.12", new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_12_IMAGE)),
            Arguments.arguments("0.11", new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_11_IMAGE)),
            Arguments.arguments(
                "0.11 with legacy = off",
                new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_11_IMAGE, false)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("localstackVersionWithLegacyOff")
    void samePortIsExposedForAllServices(String description, LocalStackContainer localstack) {
        localstack.withServices(Service.S3, Service.SQS);
        localstack.start();

        try {
            assertThat(localstack.getExposedPorts()).as("A single port is exposed").hasSize(1);
            assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                .as("Endpoint overrides are different")
                .isEqualTo(localstack.getEndpointOverride(Service.S3).toString());
            assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                .as("Endpoint configuration have different endpoints")
                .isEqualTo(localstack.getEndpointOverride(Service.S3).toString());
        } finally {
            localstack.stop();
        }
    }

    public static Stream<Arguments> localstackVersionWithLegacyOn() {
        return Stream.of(
            Arguments.arguments("0.10", new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_10_IMAGE)),
            Arguments.arguments("custom", new LocalStackContainer(LOCALSTACK_CUSTOM_TAG)),
            Arguments.arguments(
                "0.11 with legacy = on",
                new LocalStackContainer(LocalstackTestImages.LOCALSTACK_0_11_IMAGE, true)
            )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("localstackVersionWithLegacyOn")
    void differentPortsAreExposed(String description, LocalStackContainer localstack) {
        localstack.withServices(Service.S3, Service.SQS);
        localstack.start();

        try {
            assertThat(localstack.getExposedPorts()).as("Multiple ports are exposed").hasSizeGreaterThan(1);
            assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                .as("Endpoint overrides are different")
                .isNotEqualTo(localstack.getEndpointOverride(Service.S3).toString());
            assertThat(localstack.getEndpointOverride(Service.SQS).toString())
                .as("Endpoint configuration have different endpoints")
                .isNotEqualTo(localstack.getEndpointOverride(Service.S3).toString());
        } finally {
            localstack.stop();
        }
    }

    public static Stream<Arguments> constructors() {
        return Stream.of(
            Arguments.arguments("latest", false),
            Arguments.arguments("s3-latest", false),
            Arguments.arguments("latest-bigdata", false),
            Arguments.arguments("3.4.0-bigdata", false),
            Arguments.arguments("3.4.0@sha256:54fcf172f6ff70909e1e26652c3bb4587282890aff0d02c20aa7695469476ac0", false),
            Arguments.arguments("1.4@sha256:7badf31c550f81151c485980e17542592942d7f05acc09723c5f276d41b5927d", false),
            Arguments.arguments("3.4.0", false),
            Arguments.arguments("0.12", false),
            Arguments.arguments("0.11", false),
            Arguments.arguments("sha256:8bf0d744fea26603f2b11ef7206edb38375ef954258afaeda96532a6c9c1ab8b", false),
            Arguments.arguments("0.10.7@sha256:45ef287e29af7285c6e4013fafea1e3567c167cd22d12282f0a5f9c7894b1c5f", true),
            Arguments.arguments("0.10.7", true),
            Arguments.arguments("0.9.6", true)
        );
    }

    @ParameterizedTest
    @MethodSource("constructors")
    void testLegacyMode(String version, boolean shouldUseLegacyMode) {
        assertThat(LocalStackContainer.shouldRunInLegacyMode(version)).isEqualTo(shouldUseLegacyMode);
    }
}
