package org.testcontainers.dynamodb;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.testcontainers.containers.GenericContainer;

/**
 * Container for DynamoDB Local - https://hub.docker.com/r/amazon/dynamodb-local/
 */
public class DynamoDbLocalContainer extends GenericContainer<DynamoDbLocalContainer> {


    private static final String IMAGE_NAME = "amazon/dynamodb-local:1.11.119";
    private static final int MAPPED_PORT = 8000;

    public DynamoDbLocalContainer() {
        this(IMAGE_NAME);
    }

    public DynamoDbLocalContainer(final String imageName) {
        super(imageName);
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
        return new AwsClientBuilder.EndpointConfiguration(getEndpoint(), null);
    }

    /**
     * Gets an {@link AWSCredentialsProvider} that may be used to connect to this container.
     *
     * @return dummy AWS credentials
     */
    public AWSCredentialsProvider getCredentials() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy"));
    }

    /**
     * Get the AWS endpoint-uri
     *
     * @return endpoint uri
     */
    public String getEndpoint() {
        return "http://" + this.getContainerIpAddress() + ":" + this.getMappedPort(MAPPED_PORT);
    }
}
