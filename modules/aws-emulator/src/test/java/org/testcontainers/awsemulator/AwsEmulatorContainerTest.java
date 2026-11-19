package org.testcontainers.awsemulator;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.SdkAdvancedClientOption;
import software.amazon.awssdk.http.TlsTrustManagersProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class AwsEmulatorContainerTest {

    @Test
    void testS3OperationsWithExtractedTrustStoreAndCredentials() throws Exception {
        try (
            // container {
            AwsEmulatorContainer emulator = new AwsEmulatorContainer()
            // }
        ) {
            emulator.start();

            assertThat(emulator.getEndpoint().toString()).startsWith("https://");
            assertThat(emulator.getTrustStorePath()).exists();
            assertThat(emulator.getAccessKey()).isNotBlank();
            assertThat(emulator.getSecretKey()).isNotBlank();

            TlsTrustManagersProvider trustManagersProvider = createTrustManagersProvider(
                emulator.getTrustStorePath(),
                emulator.getTrustStoreType()
            );

            try (
                S3Client s3 = S3Client
                    .builder()
                    .endpointOverride(emulator.getEndpoint())
                    .credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(emulator.getAccessKey(), emulator.getSecretKey())
                        )
                    )
                    .region(Region.of(emulator.getRegion()))
                    .overrideConfiguration(config -> {
                        config.putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true);
                    })
                    .httpClientBuilder(
                        UrlConnectionHttpClient.builder().tlsTrustManagersProvider(trustManagersProvider)
                    )
                    .build()
            ) {
                s3.createBucket(CreateBucketRequest.builder().bucket("test-bucket").build());
                ListBucketsResponse buckets = s3.listBuckets();
                assertThat(buckets.buckets()).extracting("name").contains("test-bucket");
            }
        }
    }

    @Test
    void testStsCallerIdentity() throws Exception {
        try (AwsEmulatorContainer emulator = new AwsEmulatorContainer().withRegion("us-west-2")) {
            emulator.start();

            TlsTrustManagersProvider trustManagersProvider = createTrustManagersProvider(
                emulator.getTrustStorePath(),
                emulator.getTrustStoreType()
            );

            try (
                StsClient sts = StsClient
                    .builder()
                    .endpointOverride(emulator.getEndpoint())
                    .credentialsProvider(
                        StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(emulator.getAccessKey(), emulator.getSecretKey())
                        )
                    )
                    .region(Region.of(emulator.getRegion()))
                    .overrideConfiguration(config -> {
                        config.putAdvancedOption(SdkAdvancedClientOption.DISABLE_HOST_PREFIX_INJECTION, true);
                    })
                    .httpClientBuilder(
                        UrlConnectionHttpClient.builder().tlsTrustManagersProvider(trustManagersProvider)
                    )
                    .build()
            ) {
                GetCallerIdentityResponse identity = sts.getCallerIdentity();
                assertThat(identity.account()).isEqualTo("000000000000");
                assertThat(identity.arn()).contains("emulator");
            }
        }
    }

    @Test
    void testWithAuthDisabled() throws Exception {
        try (
            AwsEmulatorContainer emulator = new AwsEmulatorContainer()
                .withAuthEnabled(false)
                .withTelemetryEnabled(false)
        ) {
            emulator.start();

            assertThat(emulator.getAccessKey()).isEqualTo("test");
            assertThat(emulator.getSecretKey()).isEqualTo("test");
            assertThat(emulator.getEnvMap()).containsEntry("EMULATOR_TELEMETRY", "off");
        }
    }

    private static TlsTrustManagersProvider createTrustManagersProvider(Path trustStorePath, String trustStoreType)
        throws Exception {
        KeyStore keyStore = KeyStore.getInstance(trustStoreType);
        try (InputStream is = Files.newInputStream(trustStorePath)) {
            keyStore.load(is, new char[0]);
        }
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(keyStore);
        TrustManager[] trustManagers = tmf.getTrustManagers();
        return () -> trustManagers;
    }
}
