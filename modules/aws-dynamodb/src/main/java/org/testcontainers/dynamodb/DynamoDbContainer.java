package org.testcontainers.dynamodb;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Container for official AWS DynamoDB.
 */
public class DynamoDbContainer extends GenericContainer<DynamoDbContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME =
        DockerImageName.parse("amazon/dynamodb-local");
    private static final int MAPPED_PORT = 8000;

    public DynamoDbContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public DynamoDbContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(MAPPED_PORT);
    }

    /**
     * Gets a preconfigured {@link DynamoDbClient} client object for connecting to this container.
     *
     * @return preconfigured {@link DynamoDbClient}
     */
    public DynamoDbClient getClient() {
        return DynamoDbClient.builder()
            .region(Region.AWS_GLOBAL)
            .credentialsProvider(getCredentials())
            .endpointOverride(getEndpointURI())
            .build();
    }

    /**
     * Gets {@link URI} that may be used to connect to this container.
     *
     * @return a {@link URI} pointing to this container
     */
    public URI getEndpointURI() {
        try {
            return new URI("http://" +
                this.getHost() + ":" +
                this.getMappedPort(MAPPED_PORT));
        } catch (URISyntaxException exc) {
            throw new IllegalStateException("URI cannot be generated", exc);
        }
    }

    /**
     * Gets an {@link StaticCredentialsProvider} that may be used to connect to this container.
     *
     * @return dummy {@link StaticCredentialsProvider} to connect to this container
     */
    public StaticCredentialsProvider getCredentials() {
        return StaticCredentialsProvider.create(
            AwsBasicCredentials.create("dummy", "dummy"));
    }

}
