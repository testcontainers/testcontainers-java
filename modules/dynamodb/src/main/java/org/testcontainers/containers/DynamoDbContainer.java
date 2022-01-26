package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import static org.testcontainers.containers.DynamoDbConfig.DEFAULT_COMMAND;
import static org.testcontainers.containers.DynamoDbConfig.DEFAULT_PORT;

/**
 * TestContainer for AWS DynamoDB.
 *
 * @author Claudenir Freitas
 */
public class DynamoDbContainer extends GenericContainer<DynamoDbContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("amazon/dynamodb-local");
    private static final String DEFAULT_TAG = "1.18.0";

    private DynamoDbConfig config;

    public DynamoDbContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public DynamoDbContainer(final String version) {
        this(DEFAULT_IMAGE_NAME.withTag(version));
    }

    public DynamoDbContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        waitingFor(new HostPortWaitStrategy());
    }

    public DynamoDbContainer withConfig(final DynamoDbConfig config) {
        this.config = config;
        return this;
    }

    public String getEndpointUrl() {
        return String.format("http://%s:%d", getHost(), getMappedPort(getPort()));
    }

    @Override
    protected void configure() {
        if (config != null) {
            String command = config.toString();
            setCommand(command);
        } else {
            setCommand(DEFAULT_COMMAND);
        }
        addExposedPorts(getPort());
    }

    private int getPort() {
        if (config != null) {
            Integer port = config.getPort();
            if (port != null) {
                return port;
            }
        }
        return DEFAULT_PORT;
    }

}
