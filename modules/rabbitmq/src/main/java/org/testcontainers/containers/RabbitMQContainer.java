package org.testcontainers.containers;

import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.binding;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.exchange;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.operatorPolicy;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.parameter;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.permission;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.policy;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.queue;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.user;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.vhost;
import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.vhostLimit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testcontainers.containers.rabbitmq.admin.DeclareCommand;
import org.testcontainers.containers.rabbitmq.admin.DeclareCommands;
import org.testcontainers.containers.rabbitmq.plugins.PluginsEnableCommand;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import com.github.dockerjava.api.command.InspectContainerResponse;

/**
 * Testcontainer for RabbitMQ.
 *
 * @author Martin Greber
 */
public class RabbitMQContainer extends GenericContainer<RabbitMQContainer> {

    /**
     * The image defaults to the official RabbitmQ image: <a href="https://hub.docker.com/_/rabbitmq/">RabbitMQ</a>.
     */
    private static final String DEFAULT_IMAGE_NAME = "rabbitmq";
    private static final String DEFAULT_TAG = "3.7-management-alpine";

    private static final int DEFAULT_AMQP_PORT = 5672;
    private static final int DEFAULT_AMQPS_PORT = 5671;
    private static final int DEFAULT_HTTPS_PORT = 15671;
    private static final int DEFAULT_HTTP_PORT = 15672;

    private String adminPassword = "guest";
    private String adminUsername = "guest";
    private final List<RabbitMQCommand> commands = new ArrayList<>();

    /**
     * Creates a Testcontainer using the official RabbitMQ docker image.
     */
    public RabbitMQContainer() {
        this(DEFAULT_IMAGE_NAME + ":" + DEFAULT_TAG);
    }

    /**
     * Creates a Testcontainer using a specific docker image.
     *
     * @param image The docker image to use.
     */
    public RabbitMQContainer(String image) {
        super(image);

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
        commands.forEach(command -> command.executeInContainer(this));
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
        return "amqp://" + getContainerIpAddress() + ":" + getAmqpPort();
    }

    /**
     * @return AMQPS URL for use with AMQPS clients.
     */
    public String getAmqpsUrl() {
        return "amqps://" + getContainerIpAddress() + ":" + getAmqpsPort();
    }

    /**
     * @return URL of the HTTP management endpoint.
     */
    public String getHttpUrl() {
        return "http://" + getContainerIpAddress() + ":" + getHttpPort();
    }

    /**
     * @return URL of the HTTPS management endpoint.
     */
    public String getHttpsUrl() {
        return "https://" + getContainerIpAddress() + ":" + getHttpsPort();
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
        commands.add(new PluginsEnableCommand(pluginNames));
        return self();
    }

    /**
     * Adds the given {@link DeclareCommand} to the list of commands that will execute
     * when the RabbitMQ container starts.
     *
     * <p>The {@link DeclareCommand} will declare an object on the RabbitMQ server when the container starts.</p>
     *
     * <p>Use the static factory methods from {@link DeclareCommands} to create the {@code declareCommand}.</p>
     *
     * <p>For example:</p>
     * <pre>
     *     import static org.testcontainers.containers.rabbitmq.admin.DeclareCommands.queue;
     *
     *     container.declare(queue("myQueue").autoDelete());
     * </pre>
     *
     * @param declareCommand declares a RabbitMQ object when the container starts
     * @return this container
     */
    public RabbitMQContainer declare(DeclareCommand declareCommand) {
        commands.add(declareCommand);
        return self();
    }

    public RabbitMQContainer withBinding(String source, String destination) {
        return declare(binding(source, destination));
    }

    public RabbitMQContainer withBinding(String source, String destination, Map<String, Object> arguments, String routingKey, String destinationType) {
        return declare(binding(source, destination)
            .arguments(arguments)
            .routingKey(routingKey)
            .destinationType(destinationType));
    }

    public RabbitMQContainer withParameter(String component, String name, String value) {
        return declare(parameter(component, name, value));
    }

    public RabbitMQContainer withPermission(String vhost, String user, String configure, String write, String read) {
        return declare(permission(user)
            .vhost(vhost)
            .configure(configure)
            .write(write)
            .read(read));
    }

    public RabbitMQContainer withUser(String name, String password) {
        return declare(user(name)
            .password(password));
    }

    public RabbitMQContainer withUser(String name, String password, Set<String> tags) {
        return declare(user(name)
            .password(password)
            .tags(tags));
    }

    public RabbitMQContainer withPolicy(String name, String pattern, Map<String, Object> definition) {
        return declare(policy(name)
            .pattern(pattern)
            .definition(definition));
    }

    public RabbitMQContainer withPolicy(String name, String pattern, Map<String, Object> definition, int priority, String applyTo) {
        return declare(policy(name)
            .pattern(pattern)
            .definition(definition)
            .priority(priority)
            .applyTo(applyTo));
    }

    public RabbitMQContainer withOperatorPolicy(String name, String pattern, Map<String, Object> definition) {
        return declare(operatorPolicy(name)
            .pattern(pattern)
            .definition(definition));
    }

    public RabbitMQContainer withOperatorPolicy(String name, String pattern, Map<String, Object> definition, int priority, String applyTo) {
        return declare(operatorPolicy(name)
            .pattern(pattern)
            .definition(definition)
            .priority(priority)
            .applyTo(applyTo));
    }

    public RabbitMQContainer withVhost(String name) {
        return declare(vhost(name));
    }

    public RabbitMQContainer withVhost(String name, boolean tracing) {
        return declare(vhost(name)
            .tracing(tracing));
    }

    public RabbitMQContainer withVhostLimit(String vhost, String name, int value) {
        return declare(vhostLimit(name, value)
            .vhost(vhost));
    }

    public RabbitMQContainer withQueue(String name) {
        return declare(queue(name));
    }

    public RabbitMQContainer withQueue(String name, boolean autoDelete, boolean durable, Map<String, Object> arguments) {
        return declare(queue(name)
            .autoDelete(autoDelete)
            .durable(durable)
            .arguments(arguments));
    }

    public RabbitMQContainer withExchange(String name, String type) {
        return declare(exchange(name)
            .type(type));
    }

    public RabbitMQContainer withExchange(String name, String type, boolean autoDelete, boolean internal, boolean durable, Map<String, Object> arguments) {
        return declare(exchange(name)
            .type(type)
            .autoDelete(autoDelete)
            .internal(internal)
            .durable(durable)
            .arguments(arguments));
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

}
