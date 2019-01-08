package org.testcontainers.dynamodb;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

/**
 * Container for Dynalite, a DynamoDB clone.
 */
public class DynaliteContainer extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(DynaliteContainer.class);
    private final GenericContainer delegate;

    public DynaliteContainer() {
        this("quay.io/testcontainers/dynalite:v1.2.1-1");
    }

    public DynaliteContainer(String imageName) {
        this.delegate = new GenericContainer(imageName)
                .withExposedPorts(4567)
                .withLogConsumer(new Slf4jLogConsumer(LOGGER));
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
                this.delegate.getContainerIpAddress() + ":" +
                this.delegate.getMappedPort(4567), null);
    }

    /**
     * Gets an {@link AWSCredentialsProvider} that may be used to connect to this container.
     *
     * @return dummy AWS credentials
     */
    public AWSCredentialsProvider getCredentials() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("dummy", "dummy"));
    }

    @Override
    protected void before() throws Throwable {
        delegate.start();
    }

    @Override
    protected void after() {
        delegate.stop();
    }
}
