package org.testcontainers.containers.localstack;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import org.jetbrains.annotations.Nullable;
import org.junit.rules.ExternalResource;
import org.rnorth.ducttape.Preconditions;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;

import static org.testcontainers.containers.BindMode.READ_WRITE;

/**
 * <p>Container for Atlassian Labs Localstack, 'A fully functional local AWS cloud stack'.</p>
 * <p>{@link LocalStackContainer#withServices(Service...)} should be used to select which services
 * are to be launched. See {@link Service} for available choices. It is advised that
 * {@link LocalStackContainer#getEndpointConfiguration(Service)} and
 * {@link LocalStackContainer#getDefaultCredentialsProvider()}
 * be used to obtain compatible endpoint configuration and credentials, respectively.</p>
 */
public class LocalStackContainer extends ExternalResource {

    @Nullable private GenericContainer delegate;
    private Service[] services;

    @Override
    protected void before() throws Throwable {

        Preconditions.check("services list must not be empty", services != null && services.length > 0);

        final String servicesList = Arrays
                .stream(services)
                .map(Service::getLocalStackName)
                .collect(Collectors.joining(","));

        final Integer[] portsList = Arrays
                .stream(services)
                .map(Service::getPort)
                .collect(Collectors.toSet()).toArray(new Integer[]{});

        delegate = new GenericContainer("localstack/localstack:0.8.5")
                       .withExposedPorts(portsList)
                       .withFileSystemBind("//var/run/docker.sock", "/var/run/docker.sock", READ_WRITE)
                       .waitingFor(new LogMessageWaitStrategy().withRegEx(".*Ready\\.\n"))
                       .withEnv("SERVICES", servicesList);

        delegate.start();
    }

    @Override
    protected void after() {

        Preconditions.check("delegate must have been created by before()", delegate != null);

        delegate.stop();
    }

    /**
     * Declare a set of simulated AWS services that should be launched by this container.
     * @param services one or more service names
     * @return this container object
     */
    public LocalStackContainer withServices(Service... services) {
        this.services = services;
        return this;
    }

    /**
     * Provides an endpoint configuration that is preconfigured to communicate with a given simulated service.
     * The provided endpoint configuration should be set in the AWS Java SDK when building a client, e.g.:
     * <pre><code>AmazonS3 s3 = AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();
     </code></pre>
     * @param service the service that is to be accessed
     * @return an {@link AwsClientBuilder.EndpointConfiguration}
     */
    public AwsClientBuilder.EndpointConfiguration getEndpointConfiguration(Service service) {

        if (delegate == null) {
            throw new IllegalStateException("LocalStack has not been started yet!");
        }

        final String address = delegate.getContainerIpAddress();
        String ipAddress = address;
        try {
            ipAddress = InetAddress.getByName(address).getHostAddress();
        } catch (UnknownHostException ignored) {

        }
        ipAddress = ipAddress + ".xip.io";
        while (true) {
            try {
                //noinspection ResultOfMethodCallIgnored
                InetAddress.getAllByName(ipAddress);
                break;
            } catch (UnknownHostException ignored) {

            }
        }

        return new AwsClientBuilder.EndpointConfiguration(
                "http://" +
                ipAddress +
                ":" +
                delegate.getMappedPort(service.getPort()), "us-east-1");
    }

    /**
     * Provides a {@link AWSCredentialsProvider} that is preconfigured to communicate with a given simulated service.
     * The credentials provider should be set in the AWS Java SDK when building a client, e.g.:
     * <pre><code>AmazonS3 s3 = AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
            .withCredentials(localstack.getDefaultCredentialsProvider())
            .build();
     </code></pre>
     * @return an {@link AWSCredentialsProvider}
     */
    public AWSCredentialsProvider getDefaultCredentialsProvider() {
        return new AWSStaticCredentialsProvider(new BasicAWSCredentials("accesskey", "secretkey"));
    }

    public enum Service {
        API_GATEWAY("apigateway",             4567),
        KINESIS("kinesis",                 4568),
        DYNAMODB("dynamodb",        4569),
        DYNAMODB_STREAMS("dynamodbstreams",        4570),
        // TODO: Clarify usage for ELASTICSEARCH and ELASTICSEARCH_SERVICE
//        ELASTICSEARCH("es",           4571),
        S3("s3",                    4572),
        FIREHOSE("firehose",                4573),
        LAMBDA("lambda",                  4574),
        SNS("sns",                     4575),
        SQS("sqs",                     4576),
        REDSHIFT("redshift",                4577),
//        ELASTICSEARCH_SERVICE("",   4578),
        SES("ses",                     4579),
        ROUTE53("route53",                 4580),
        CLOUDFORMATION("cloudformation",          4581),
        CLOUDWATCH("cloudwatch",              4582);

        private final String localStackName;
        private final int port;

        Service(String localstackName, int port) {
            this.localStackName = localstackName;
            this.port = port;
        }

        public String getLocalStackName() {
            return localStackName;
        }

        public Integer getPort() { return port; }
    }
}
