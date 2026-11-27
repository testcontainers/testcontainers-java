package org.testcontainers.awsemulator;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Future;

/**
 * Testcontainers implementation for AWS Emulator.
 *
 * <p>Supported images: {@code aws-emulator} (or built automatically from the official AWS Linux RPM package)
 *
 * <p>Exposed ports: 9297 (HTTPS)
 */
public class AwsEmulatorContainer extends GenericContainer<AwsEmulatorContainer> {

    static final int PORT = 9297;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("aws-emulator");

    private static final String BASE_IMAGE =
        "public.ecr.aws/amazonlinux/amazonlinux@sha256:158a9044f9dc0777f098b76c8b54346f6ed4560b9a6a416dec7a5ba3bc483af4";

    private static final String RPM_URL_X86_64 =
        "https://downloads.aws.com/aws-emulator/packages/aws-emulator/LTS/latest/aws-emulator-x86_64.rpm";

    private static final String RPM_SHA256_X86_64 = "0aa130ff811add64826c6811ec714e375d4a7891d08976ae57bdb6ee645724ee";

    private static final String RPM_URL_AARCH64 =
        "https://downloads.aws.com/aws-emulator/packages/aws-emulator/LTS/latest/aws-emulator-aarch64.rpm";

    private static final String RPM_SHA256_AARCH64 = "38e2069c656429eaa023fdc807a49c02a48acff77f090f068ccdc9fa0a7c4fe4";

    private static final String DEFAULT_REGION = "us-east-1";

    private static final long TWO_GIB_IN_BYTES = 2L * 1024 * 1024 * 1024;

    private static final String EMULATOR_DATA_DIR = "/home/emulator/.aws/emulator/" + PORT;

    private boolean telemetryEnabled = false;

    private boolean authEnabled = true;

    private long memoryLimitBytes = TWO_GIB_IN_BYTES;

    private String region = DEFAULT_REGION;

    private String accessKey;

    private String secretKey;

    private Path trustStorePath;

    /**
     * Creates an {@link AwsEmulatorContainer} by building the image on-the-fly from the official
     * Amazon Linux 2023 minimal base image and AWS Emulator RPM package.
     */
    public AwsEmulatorContainer() {
        super(createDefaultImage());
        initContainer();
    }

    /**
     * Creates an {@link AwsEmulatorContainer} from a prebuilt Docker image name.
     *
     * @param dockerImageName image name to use for AWS Emulator
     */
    public AwsEmulatorContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Creates an {@link AwsEmulatorContainer} from a prebuilt {@link DockerImageName}.
     *
     * @param dockerImageName image name to use for AWS Emulator
     */
    public AwsEmulatorContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        initContainer();
    }

    private static Future<String> createDefaultImage() {
        boolean isArm64 = isArm64Daemon();
        String rpmUrl = isArm64 ? RPM_URL_AARCH64 : RPM_URL_X86_64;
        String expectedSha256 = isArm64 ? RPM_SHA256_AARCH64 : RPM_SHA256_X86_64;

        return new ImageFromDockerfile("testcontainers/aws-emulator", false)
            .withDockerfileFromBuilder(builder -> {
                builder
                    .from(BASE_IMAGE)
                    .add(rpmUrl, "/tmp/aws-emulator.rpm")
                    .run(
                        "echo \"" +
                        expectedSha256 +
                        "  /tmp/aws-emulator.rpm\" | sha256sum -c - && rpm -i /tmp/aws-emulator.rpm && rm /tmp/aws-emulator.rpm && mkdir -p /home/emulator && chown 1000 /home/emulator"
                    )
                    .user("1000")
                    .env("HOME", "/home/emulator")
                    .expose(PORT)
                    .entryPoint("aws-emulator", "-b", "0.0.0.0")
                    .build();
            });
    }

    private static boolean isArm64Daemon() {
        String arch = null;
        try {
            arch = DockerClientFactory.instance().client().infoCmd().exec().getArchitecture();
        } catch (Exception ignored) {
            // Fall back to JVM architecture if Docker daemon info cannot be queried
        }
        if (arch == null || arch.trim().isEmpty()) {
            arch = System.getProperty("os.arch", "");
        }
        String normalized = arch.toLowerCase(Locale.ROOT);
        return normalized.contains("aarch64") || normalized.contains("arm64");
    }

    private void initContainer() {
        withExposedPorts(PORT);
        waitingFor(
            Wait
                .forHttp("/__admin/health")
                .forPort(PORT)
                .usingTls()
                .allowInsecure()
                .forStatusCode(200)
                .forResponsePredicate("ok"::equals)
        );
    }

    @Override
    protected void configure() {
        withEnv("EMULATOR_TELEMETRY", this.telemetryEnabled ? "on" : "off");
        withEnv("AWS_REGION", this.region);
        withEnv("AWS_DEFAULT_REGION", this.region);

        List<String> cmdArgs = new ArrayList<>();
        cmdArgs.add("-p");
        cmdArgs.add(String.valueOf(PORT));
        if (!this.authEnabled) {
            cmdArgs.add("--auth");
            cmdArgs.add("off");
        }
        withCommand(cmdArgs.toArray(new String[0]));

        withCreateContainerCmdModifier(cmd -> {
            if (cmd.getHostConfig() != null) {
                cmd.getHostConfig().withMemory(this.memoryLimitBytes).withMemorySwap(this.memoryLimitBytes);
            }
        });
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        extractTrustStore();
        if (this.authEnabled) {
            extractCredentials();
        }
    }

    private void extractTrustStore() {
        try {
            Path tempTrustStore = Files.createTempFile("aws-emulator-truststore-", ".p12");
            tempTrustStore.toFile().deleteOnExit();
            copyFileFromContainer(EMULATOR_DATA_DIR + "/truststore.p12", tempTrustStore.toAbsolutePath().toString());
            this.trustStorePath = tempTrustStore;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract truststore.p12 from AWS Emulator container", e);
        }
    }

    private void extractCredentials() {
        copyFileFromContainer(
            EMULATOR_DATA_DIR + "/emulator_credentials",
            inputStream -> {
                try (
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8)
                    )
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String trimmed = line.trim();
                        if (trimmed.startsWith("aws_access_key_id")) {
                            this.accessKey = extractIniValue(trimmed);
                        } else if (trimmed.startsWith("aws_secret_access_key")) {
                            this.secretKey = extractIniValue(trimmed);
                        }
                    }
                }
                return null;
            }
        );
    }

    private static String extractIniValue(String line) {
        int idx = line.indexOf('=');
        if (idx < 0 || idx == line.length() - 1) {
            return null;
        }
        return line.substring(idx + 1).trim();
    }

    /**
     * Enables or disables usage telemetry in the AWS Emulator (disabled by default).
     *
     * @param telemetryEnabled {@code true} to enable telemetry, {@code false} to disable
     * @return this container instance
     */
    public AwsEmulatorContainer withTelemetryEnabled(boolean telemetryEnabled) {
        this.telemetryEnabled = telemetryEnabled;
        return self();
    }

    /**
     * Enables or disables SigV4 request signature verification (enabled by default).
     *
     * @param authEnabled {@code true} to require SigV4 authentication, {@code false} to disable
     * @return this container instance
     */
    public AwsEmulatorContainer withAuthEnabled(boolean authEnabled) {
        this.authEnabled = authEnabled;
        return self();
    }

    /**
     * Sets the memory limit (and matching memory-swap limit) in bytes for the container.
     * Defaults to 2 GiB as recommended by AWS Emulator.
     *
     * @param memoryLimitBytes memory limit in bytes (must be positive)
     * @return this container instance
     */
    public AwsEmulatorContainer withMemoryLimit(long memoryLimitBytes) {
        if (memoryLimitBytes <= 0) {
            throw new IllegalArgumentException("memoryLimitBytes must be greater than 0");
        }
        this.memoryLimitBytes = memoryLimitBytes;
        return self();
    }

    /**
     * Sets the default AWS region for the emulator.
     *
     * @param region AWS region identifier (e.g., {@code us-east-1})
     * @return this container instance
     */
    public AwsEmulatorContainer withRegion(String region) {
        if (region == null || region.trim().isEmpty()) {
            throw new IllegalArgumentException("region must not be null or empty");
        }
        this.region = region.trim();
        return self();
    }

    /**
     * Returns the HTTPS endpoint URI to configure on AWS SDK v2 clients.
     *
     * @return the HTTPS endpoint {@link URI}
     */
    public URI getEndpoint() {
        try {
            String ipAddress = InetAddress.getByName(getHost()).getHostAddress();
            return new URI("https", null, ipAddress, getMappedPort(PORT), null, null, null);
        } catch (UnknownHostException | URISyntaxException e) {
            throw new IllegalStateException("Failed to build AWS Emulator endpoint URI", e);
        }
    }

    /**
     * Returns the generated AWS access key ID for this emulator run, or {@code "test"} if auth is disabled.
     *
     * @return the AWS access key ID
     */
    public String getAccessKey() {
        return this.accessKey != null ? this.accessKey : "test";
    }

    /**
     * Returns the generated AWS secret access key for this emulator run, or {@code "test"} if auth is disabled.
     *
     * @return the AWS secret access key
     */
    public String getSecretKey() {
        return this.secretKey != null ? this.secretKey : "test";
    }

    /**
     * Returns the configured AWS region.
     *
     * @return the AWS region
     */
    public String getRegion() {
        return this.region;
    }

    /**
     * Returns the local path to the extracted PKCS12 truststore ({@code truststore.p12})
     * containing the emulator's self-signed TLS certificate.
     *
     * @return path to {@code truststore.p12}
     */
    public Path getTrustStorePath() {
        return this.trustStorePath;
    }

    /**
     * Returns the truststore type ({@code PKCS12}) for use with Java SSL configuration.
     *
     * @return {@code "PKCS12"}
     */
    public String getTrustStoreType() {
        return "PKCS12";
    }
}
