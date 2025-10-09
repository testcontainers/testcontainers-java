package org.testcontainers.localstack;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Testcontainers implementation for LocalStack.
 * <p>
 * Supported images: {@code localstack/localstack}, {@code localstack/localstack-pro}
 * <p>
 * Exposed ports: 4566
 */
@Slf4j
public class LocalStackContainer extends GenericContainer<LocalStackContainer> {

    static final int PORT = 4566;

    private final List<String> services = new ArrayList<>();

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("localstack/localstack");

    private static final DockerImageName LOCALSTACK_PRO_IMAGE_NAME = DockerImageName.parse("localstack/localstack-pro");

    private static final String DEFAULT_REGION = "us-east-1";

    private static final String DEFAULT_AWS_ACCESS_KEY_ID = "test";

    private static final String DEFAULT_AWS_SECRET_ACCESS_KEY = "test";

    private static final String STARTER_SCRIPT = "/testcontainers_start.sh";

    /**
     * @param dockerImageName    image name to use for Localstack
     */
    public LocalStackContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * @param dockerImageName    image name to use for Localstack
     */
    public LocalStackContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, LOCALSTACK_PRO_IMAGE_NAME);

        withExposedPorts(PORT);
        withFileSystemBind(DockerClientFactory.instance().getRemoteDockerUnixSocketPath(), "/var/run/docker.sock");
        waitingFor(Wait.forLogMessage(".*Ready\\.\n", 1));
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint(
                "sh",
                "-c",
                "while [ ! -f " + STARTER_SCRIPT + " ]; do sleep 0.1; done; " + STARTER_SCRIPT
            );
        });
    }

    @Override
    protected void configure() {
        if (!services.isEmpty()) {
            withEnv("SERVICES", String.join(",", this.services));
        }
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        String command = "#!/bin/bash\n";
        command += "export LAMBDA_DOCKER_FLAGS=" + configureServiceContainerLabels("LAMBDA_DOCKER_FLAGS") + "\n";
        command += "export ECS_DOCKER_FLAGS=" + configureServiceContainerLabels("ECS_DOCKER_FLAGS") + "\n";
        command += "export EC2_DOCKER_FLAGS=" + configureServiceContainerLabels("EC2_DOCKER_FLAGS") + "\n";
        command += "export BATCH_DOCKER_FLAGS=" + configureServiceContainerLabels("BATCH_DOCKER_FLAGS") + "\n";
        command += "/usr/local/bin/docker-entrypoint.sh\n";
        copyFileToContainer(Transferable.of(command, 0777), STARTER_SCRIPT);
    }

    /**
     * Configure the LocalStack container to include the default testcontainers labels on all spawned lambda containers
     * Necessary to properly clean up lambda containers even if the LocalStack container is killed before it gets the
     * chance.
     * @return the lambda container labels as a string
     */
    private String configureServiceContainerLabels(String existingEnvFlagKey) {
        String internalMarkerFlags = internalMarkerLabels();
        String existingFlags = getEnvMap().get(existingEnvFlagKey);
        if (existingFlags != null) {
            internalMarkerFlags = existingFlags + " " + internalMarkerFlags;
        }
        return "\"" + internalMarkerFlags + "\"";
    }

    /**
     * Provides a docker argument string including all default labels set on testcontainers containers (excluding reuse labels)
     * @return Argument string in the format `-l key1=value1 -l key2=value2`
     */
    private String internalMarkerLabels() {
        return getContainerInfo()
            .getConfig()
            .getLabels()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().startsWith(DockerClientFactory.TESTCONTAINERS_LABEL))
            .filter(entry -> {
                return (
                    !entry.getKey().equals("org.testcontainers.hash") &&
                    !entry.getKey().equals("org.testcontainers.copied_files.hash")
                );
            })
            .map(entry -> String.format("-l %s=%s", entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(" "));
    }

    /**
     * Declare a set of simulated AWS services that should be launched by this container.
     * @param services one or more service names
     * @return this container object
     */
    public LocalStackContainer withServices(String... services) {
        this.services.addAll(Arrays.asList(services));
        return self();
    }

    /**
     * Provides an endpoint to communicate with LocalStack service.
     * The provided endpoint should be set in the AWS Java SDK v2 when building a client, e.g.:
     * <pre><code>S3Client s3 = S3Client
             .builder()
             .endpointOverride(localstack.getEndpoint())
             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
             localstack.getAccessKey(), localstack.getSecretKey()
             )))
             .region(Region.of(localstack.getRegion()))
             .build()
             </code></pre>
     * <p><strong>Please note that this method is only intended to be used for configuring AWS SDK clients
     * that are running on the test host. If other containers need to call this one, they should be configured
     * specifically to do so using a Docker network and appropriate addressing.</strong></p>
     *
     * @return an {@link URI} endpoint
     */
    public URI getEndpoint() {
        try {
            final String address = getHost();
            // resolve IP address and use that as the endpoint so that path-style access is automatically used for S3
            String ipAddress = InetAddress.getByName(address).getHostAddress();
            return new URI("http://" + ipAddress + ":" + getMappedPort(PORT));
        } catch (UnknownHostException | URISyntaxException e) {
            throw new IllegalStateException("Cannot obtain endpoint URL", e);
        }
    }

    /**
     * Provides a default access key that is preconfigured to communicate with a given simulated service.
     * <a href="https://github.com/localstack/localstack/blob/master/doc/interaction/README.md?plain=1#L32">AWS Access Key</a>
     * The access key can be used to construct AWS SDK v2 clients:
     * <pre><code>S3Client s3 = S3Client
             .builder()
             .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
             localstack.getAccessKey(), localstack.getSecretKey()
             )))
             .region(Region.of(localstack.getRegion()))
             .build()
     </code></pre>
     * @return a default access key
     */
    public String getAccessKey() {
        return this.getEnvMap().getOrDefault("AWS_ACCESS_KEY_ID", DEFAULT_AWS_ACCESS_KEY_ID);
    }

    /**
     * Provides a default secret key that is preconfigured to communicate with a given simulated service.
     * <a href="https://github.com/localstack/localstack/blob/master/doc/interaction/README.md?plain=1#L32">AWS Secret Key</a>
     * The secret key can be used to construct AWS SDK v2 clients:
     * <pre><code>S3Client s3 = S3Client
             .builder()
             .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
             localstack.getAccessKey(), localstack.getSecretKey()
             )))
             .region(Region.of(localstack.getRegion()))
             .build()
     </code></pre>
     * @return a default secret key
     */
    public String getSecretKey() {
        return this.getEnvMap().getOrDefault("AWS_SECRET_ACCESS_KEY", DEFAULT_AWS_SECRET_ACCESS_KEY);
    }

    /**
     * Provides a default region that is preconfigured to communicate with a given simulated service.
     * The region can be used to construct AWS SDK v2 clients:
     * <pre><code>S3Client s3 = S3Client
             .builder()
             .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
             .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(
             localstack.getAccessKey(), localstack.getSecretKey()
             )))
             .region(Region.of(localstack.getRegion()))
             .build()
     </code></pre>
     * @return a default region
     */
    public String getRegion() {
        return this.getEnvMap().getOrDefault("DEFAULT_REGION", DEFAULT_REGION);
    }
}
