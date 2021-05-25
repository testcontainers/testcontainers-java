package org.testcontainers.containers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.join;
import static java.util.Arrays.asList;

/**
 * Testcontainer for RabbitMQ.
 *
 * @author Martin Greber
 */
public class RabbitMQContainer extends GenericContainer<RabbitMQContainer> {

    /**
     * The image defaults to the official RabbitmQ image: <a href="https://hub.docker.com/_/rabbitmq/">RabbitMQ</a>.
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("rabbitmq");
    private static final String DEFAULT_TAG = "3.7.25-management-alpine";

    private static final int DEFAULT_AMQP_PORT = 5672;
    private static final int DEFAULT_AMQPS_PORT = 5671;
    private static final int DEFAULT_HTTPS_PORT = 15671;
    private static final int DEFAULT_HTTP_PORT = 15672;

    private String adminPassword = "guest";
    private String adminUsername = "guest";
    private final List<List<String>> values = new ArrayList<>();

    /**
     * Creates a RabbitMQ container using the official RabbitMQ docker image.
     * @deprecated use {@link RabbitMQContainer(DockerImageName)} instead
     */
    @Deprecated
    public RabbitMQContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Creates a RabbitMQ container using a specific docker image.
     *
     * @param dockerImageName The docker image to use.
     */
    public RabbitMQContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public RabbitMQContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(DEFAULT_AMQP_PORT, DEFAULT_AMQPS_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT);

        this.waitStrategy = Wait.
                forLogMessage(".*Server startup complete.*", 1)
                .withStartupTimeout(Duration.ofSeconds(60));
    }

    @Override
    protected void configure() {
        if (adminPassword != null) {
            addEnv("RABBITMQ_DEFAULT_PASS", adminPassword);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {

        values.forEach(command -> {
            try {
                ExecResult execResult = execInContainer(command.toArray(new String[0]));
                if (execResult.getExitCode() != 0) {
                    logger().error("Could not execute command {}: {}", command, execResult.getStderr());
                }
            } catch (IOException | InterruptedException e) {
                logger().error("Could not execute command {}: {}", command, e.getMessage());
            }
        });
    }

    /**
     * @return The admin password for the <code>admin</code> account
     */
    public String getAdminPassword() {
        return adminPassword;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public Integer getAmqpPort() {
        return getMappedPort(DEFAULT_AMQP_PORT);
    }

    public Integer getAmqpsPort() {
        return getMappedPort(DEFAULT_AMQPS_PORT);
    }

    public Integer getHttpsPort() {
        return getMappedPort(DEFAULT_HTTPS_PORT);
    }

    public Integer getHttpPort() {
        return getMappedPort(DEFAULT_HTTP_PORT);
    }

    /**
     * @return AMQP URL for use with AMQP clients.
     */
    public String getAmqpUrl() {
        return "amqp://" + getHost() + ":" + getAmqpPort();
    }

    /**
     * @return AMQPS URL for use with AMQPS clients.
     */
    public String getAmqpsUrl() {
        return "amqps://" + getHost() + ":" + getAmqpsPort();
    }

    /**
     * @return URL of the HTTP management endpoint.
     */
    public String getHttpUrl() {
        return "http://" + getHost() + ":" + getHttpPort();
    }

    /**
     * @return URL of the HTTPS management endpoint.
     */
    public String getHttpsUrl() {
        return "https://" + getHost() + ":" + getHttpsPort();
    }

    /**
     * Sets the password for the admin (default is <pre>guest</pre>)
     *
     * @param adminPassword The admin password.
     * @return This container.
     */
    public RabbitMQContainer withAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
        return this;
    }

    public enum SslVerification {
        VERIFY_NONE("verify_none"), VERIFY_PEER("verify_peer");

        SslVerification(String value) {
            this.value = value;
        }
        private final String value;

    }

    public RabbitMQContainer withSSL(
            final MountableFile keyFile,
            final MountableFile certFile,
            final MountableFile caFile,
            final SslVerification verify,
            boolean failIfNoCert,
            int verificationDepth) {

        return withSSL(keyFile, certFile, caFile, verify, failIfNoCert)
                .withEnv("RABBITMQ_SSL_DEPTH", String.valueOf(verificationDepth));
    }

    public RabbitMQContainer withSSL(
            final MountableFile keyFile,
            final MountableFile certFile,
            final MountableFile caFile,
            final SslVerification verify,
            boolean failIfNoCert) {

        return withSSL(keyFile, certFile, caFile, verify)
                .withEnv("RABBITMQ_SSL_FAIL_IF_NO_PEER_CERT", String.valueOf(failIfNoCert));
    }

    public RabbitMQContainer withSSL(
            final MountableFile keyFile,
            final MountableFile certFile,
            final MountableFile caFile,
            final SslVerification verify) {

        return withEnv("RABBITMQ_SSL_CACERTFILE", "/etc/rabbitmq/ca_cert.pem")
                .withEnv("RABBITMQ_SSL_CERTFILE", "/etc/rabbitmq/rabbitmq_cert.pem")
                .withEnv("RABBITMQ_SSL_KEYFILE", "/etc/rabbitmq/rabbitmq_key.pem")
                .withEnv("RABBITMQ_SSL_VERIFY", verify.value)
                .withCopyFileToContainer(certFile, "/etc/rabbitmq/rabbitmq_cert.pem")
                .withCopyFileToContainer(caFile, "/etc/rabbitmq/ca_cert.pem")
                .withCopyFileToContainer(keyFile, "/etc/rabbitmq/rabbitmq_key.pem");
    }

    public RabbitMQContainer withPluginsEnabled(String... pluginNames) {
        List<String> command = new ArrayList<>(asList("rabbitmq-plugins", "enable"));
        command.addAll(asList(pluginNames));
        values.add(command);
        return self();
    }

    public RabbitMQContainer withBinding(String source, String destination) {
        values.add(asList("rabbitmqadmin", "declare", "binding",
                "source=" + source,
                "destination=" + destination));
        return self();
    }

    public RabbitMQContainer withBinding(String source, String destination, Map<String, Object> arguments, String routingKey, String destinationType) {
        values.add(asList("rabbitmqadmin", "declare", "binding",
                "source=" + source,
                "destination=" + destination,
                "routing_key=" + routingKey,
                "destination_type=" + destinationType,
                "arguments=" + toJson(arguments)));
        return self();
    }

    public RabbitMQContainer withParameter(String component, String name, String value) {
        values.add(asList("rabbitmqadmin", "declare", "parameter",
                "component=" + component,
                "name=" + name,
                "value=" + value));
        return self();
    }

    public RabbitMQContainer withPermission(String vhost, String user, String configure, String write, String read) {
        values.add(asList("rabbitmqadmin", "declare", "permission",
                "vhost=" + vhost,
                "user=" + user,
                "configure=" + configure,
                "write=" + write,
                "read=" + read));
        return self();
    }

    public RabbitMQContainer withUser(String name, String password) {
        values.add(asList("rabbitmqadmin", "declare", "user",
                "name=" + name,
                "password=" + password,
                "tags="));
        return self();
    }

    public RabbitMQContainer withUser(String name, String password, Set<String> tags) {
        values.add(asList("rabbitmqadmin", "declare", "user",
                "name=" + name,
                "password=" + password,
                "tags=" + join(",", tags)));
        return self();
    }

    public RabbitMQContainer withPolicy(String name, String pattern, Map<String, Object> definition) {
        values.add(asList("rabbitmqadmin", "declare", "policy",
                "name=" + name,
                "pattern=" + pattern,
                "definition=" + toJson(definition)));
        return self();
    }

    public RabbitMQContainer withPolicy(String vhost, String name, String pattern, Map<String, Object> definition) {
        values.add(asList("rabbitmqadmin", "declare", "policy",
                "--vhost=" + vhost,
                "name=" + name,
                "pattern=" + pattern,
                "definition=" + toJson(definition)));
        return self();
    }

    public RabbitMQContainer withPolicy(String name, String pattern, Map<String, Object> definition, int priority, String applyTo) {
        values.add(asList("rabbitmqadmin", "declare", "policy",
                "name=" + name,
                "pattern=" + pattern,
                "priority=" + priority,
                "apply-to=" + applyTo,
                "definition=" + toJson(definition)));
        return self();
    }

    public RabbitMQContainer withOperatorPolicy(String name, String pattern, Map<String, Object> definition) {
        values.add(new ArrayList<>(asList("rabbitmqadmin", "declare", "operator_policy",
                "name=" + name,
                "pattern=" + pattern,
                "definition=" + toJson(definition))));
        return self();
    }

    public RabbitMQContainer withOperatorPolicy(String name, String pattern, Map<String, Object> definition, int priority, String applyTo) {
        values.add(asList("rabbitmqadmin", "declare", "operator_policy",
                "name=" + name,
                "pattern=" + pattern,
                "priority=" + priority,
                "apply-to=" + applyTo,
                "definition=" + toJson(definition)));
        return self();
    }

    public RabbitMQContainer withVhost(String name) {
        values.add(asList("rabbitmqadmin", "declare", "vhost",
                "name=" + name));
        return self();
    }

    public RabbitMQContainer withVhost(String name, boolean tracing) {
        values.add(asList("rabbitmqadmin", "declare", "vhost",
                "name=" + name,
                "tracing=" + tracing));
        return self();
    }

    public RabbitMQContainer withVhostLimit(String vhost, String name, int value) {
        values.add(asList("rabbitmqadmin", "declare", "vhost_limit",
                "vhost=" + vhost,
                "name=" + name,
                "value=" + value));
        return self();
    }

    public RabbitMQContainer withQueue(String name) {
        values.add(asList("rabbitmqadmin", "declare", "queue",
                "name=" + name));
        return self();
    }

    public RabbitMQContainer withQueue(String name, boolean autoDelete, boolean durable, Map<String, Object> arguments) {
        values.add(asList("rabbitmqadmin", "declare", "queue",
                "name=" + name,
                "auto_delete=" + autoDelete,
                "durable=" + durable,
                "arguments=" + toJson(arguments)));
        return self();
    }

    public RabbitMQContainer withExchange(String name, String type) {
        values.add(asList("rabbitmqadmin", "declare", "exchange",
                "name=" + name,
                "type=" + type));
        return self();
    }

    public RabbitMQContainer withExchange(String name, String type, boolean autoDelete, boolean internal, boolean durable, Map<String, Object> arguments) {
        values.add(asList("rabbitmqadmin", "declare", "exchange",
            "name=" + name,
            "type=" + type,
            "auto_delete=" + autoDelete,
            "internal=" + internal,
            "durable=" + durable,
            "arguments=" + toJson(arguments)));
        return self();
    }

    public RabbitMQContainer withExchange(String vhost, String name, String type, boolean autoDelete, boolean internal, boolean durable, Map<String, Object> arguments) {
        values.add(asList("rabbitmqadmin", "--vhost=" + vhost, "declare", "exchange",
                "name=" + name,
                "type=" + type,
                "auto_delete=" + autoDelete,
                "internal=" + internal,
                "durable=" + durable,
                "arguments=" + toJson(arguments)));
        return self();
    }

    /**
     * Overwrites the default RabbitMQ configuration file with the supplied one.
     *
     * @param rabbitMQConf The rabbitmq.conf file to use (in sysctl format, don't forget empty line in the end of file)
     * @return This container.
     */
    public RabbitMQContainer withRabbitMQConfig(MountableFile rabbitMQConf) {

        return withRabbitMQConfigSysctl(rabbitMQConf);
    }

    /**
     * Overwrites the default RabbitMQ configuration file with the supplied one.
     *
     * This function doesn't work with RabbitMQ &lt; 3.7.
     *
     * This function and the Sysctl format is recommended for RabbitMQ &gt;= 3.7
     *
     * @param rabbitMQConf The rabbitmq.config file to use (in sysctl format, don't forget empty line in the end of file)
     * @return This container.
     */
    public RabbitMQContainer withRabbitMQConfigSysctl(MountableFile rabbitMQConf) {
        withEnv("RABBITMQ_CONFIG_FILE", "/etc/rabbitmq/rabbitmq-custom");
        return withCopyFileToContainer(rabbitMQConf, "/etc/rabbitmq/rabbitmq-custom.conf");
    }

    /**
     * Overwrites the default RabbitMQ configuration file with the supplied one.
     *
     * @param rabbitMQConf The rabbitmq.config file to use (in erlang format)
     * @return This container.
     */
    public RabbitMQContainer withRabbitMQConfigErlang(MountableFile rabbitMQConf) {
        withEnv("RABBITMQ_CONFIG_FILE", "/etc/rabbitmq/rabbitmq-custom.config");
        return withCopyFileToContainer(rabbitMQConf, "/etc/rabbitmq/rabbitmq-custom.config");
    }

    @NotNull
    private String toJson(Map<String, Object> arguments) {
        try {
            return new ObjectMapper().writeValueAsString(arguments);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to convert arguments into json: " + e.getMessage(), e);
        }
    }
}
