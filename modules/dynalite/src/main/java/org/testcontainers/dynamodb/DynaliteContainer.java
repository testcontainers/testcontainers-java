package org.testcontainers.dynamodb;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Container for Dynalite, a DynamoDB clone.
 */
public class DynaliteContainer extends GenericContainer<DynaliteContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("quay.io/testcontainers/dynalite");
    private static final String DEFAULT_TAG = "v1.2.1-1";
    private static final int MAPPED_PORT = 4567;

    /**
     * @deprecated use {@link DynaliteContainer(DockerImageName)} instead
     */
    @Deprecated
    public DynaliteContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public DynaliteContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public DynaliteContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(MAPPED_PORT);
    }


    /**
     * Gets a preconfigured {@link AmazonDynamoDB} client object for connecting to this
     * container.
     *
     * @return preconfigured client
     */
    public AmazonDynamoDB getClient() {
        return AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(getEndpointConfiguration())
                .withCredentials(getCredentials())
                .build();
    }

    /**
     * Gets {@link AwsClientBuilder.EndpointConfiguration}
     * that may be used to connect to this container.
     *
     * @return endpoint configuration
     */
    public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration() {
        return new AwsClientBuilder.EndpointConfiguration("http://" +
                this.getHost() + ":" +
                this.getMappedPort(MAPPED_PORT), null);
    }

    /**
     * Gets an {@link AWSCredentialsProvider} that may be used to connect to this container.
     *
     * @return dummy AWS credentials
     */
    public AWSCredentialsProvider getCredentials() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy"));
    }


}
