package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;

/**
 * Testcontainer for RabbitMQ.
 *
 * @author Martin Greber
 */
public class RabbitMQContainer<S extends RabbitMQContainer<S>> extends GenericContainer<S> {

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
    private List<String> plugins = emptyList();
    private List<List<String>> values = new ArrayList<>();

    /**
     * Creates a Testcontainer using the official Neo4j docker image.
     */
    public RabbitMQContainer() {
        this(DEFAULT_TAG);
    }

    /**
     * Creates a Testcontainer using a specific docker image.
     *
     * @param tag The docker image to use.
     */
    public RabbitMQContainer(String tag) {
        super(DEFAULT_IMAGE_NAME + ":" + tag);

        WaitStrategy waitForStartup = new LogMessageWaitStrategy()
                .withRegEx(String.format(".*Server startup complete.*"));

        this.waitStrategy = new WaitAllStrategy()
                .withStrategy(waitForStartup)
                .withStartupTimeout(Duration.ofSeconds(60));
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {

        return Stream.of(DEFAULT_AMQP_PORT, DEFAULT_AMQPS_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT)
                .map(this::getMappedPort)
                .collect(toSet());
    }

    @Override
    protected void configure() {
        addExposedPorts(DEFAULT_AMQP_PORT, DEFAULT_AMQPS_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT);

        if (adminPassword != null) {
            addEnv("RABBITMQ_DEFAULT_PASS", adminPassword);
        }
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {

        enablePlugins();

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

    private void enablePlugins() {
        plugins.forEach(plugin -> {
            try {
                execInContainer("rabbitmq-plugins", "enable", "--offline", plugin);
            } catch (IOException | InterruptedException e) {
                throw new ContainerConfigurationException(String.format("Could not enable plugin %s: %s", plugin, e.getMessage()));
            }
        });
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
        return String.format("amqp://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_AMQP_PORT));
    }

    /**
     * @return AMQPS URL for use with AMQPS clients.
     */
    public String getAmqpsUrl() {
        return String.format("amqps://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_AMQPS_PORT));
    }

    /**
     * @return URL of the HTTP management endpoint.
     */
    public String getHttpUrl() {
        return String.format("http://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_HTTP_PORT));
    }

    /**
     * @return URL of the HTTPS management endpoint.
     */
    public String getHttpsUrl() {
        return String.format("https://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_HTTPS_PORT));
    }

    /**
     * Sets the password for the admin (default is <pre>guest</pre>)
     *
     * @param adminPassword The admin password.
     * @return This container.
     */
    public S withAdminPassword(final String adminPassword) {
        this.adminPassword = adminPassword;
        return self();
    }

    public enum SslVerification {
        VERIFY_NONE("verify_none"), VERIFY_PEER("");

        SslVerification(String value) {
            this.value = value;
        }

        private final String value;
    }

    public S withSSL(
            final MountableFile keyFile,
            final MountableFile certFile,
            final MountableFile caFile,
            SslVerification verify,
            boolean failIfNoCert,
            int verificationDepth) {

        return withSSL(keyFile, certFile, caFile, verify, failIfNoCert)
                .withEnv("RABBITMQ_SSL_DEPTH", String.valueOf(verificationDepth));
    }

    public S withSSL(
            final MountableFile keyFile,
            final MountableFile certFile,
            final MountableFile caFile,
            SslVerification verify,
            boolean failIfNoCert) {

        return withSSL(keyFile, certFile, caFile, verify)
                .withEnv("RABBITMQ_SSL_FAIL_IF_NO_PEER_CERT", String.valueOf(failIfNoCert));
    }

    public S withSSL(
            final MountableFile keyFile,
            final MountableFile certFile,
            final MountableFile caFile,
            SslVerification verify) {

        return withEnv("RABBITMQ_SSL_CACERTFILE", "/etc/rabbitmq/ca_cert.pem")
                .withEnv("RABBITMQ_SSL_CERTFILE", "/etc/rabbitmq/rabbitmq_cert.pem")
                .withEnv("RABBITMQ_SSL_KEYFILE", "/etc/rabbitmq/rabbitmq_key.pem")
                .withEnv("RABBITMQ_SSL_VERIFY", verify.value)
                .withCopyFileToContainer(certFile, "/etc/rabbitmq/rabbitmq_cert.pem")
                .withCopyFileToContainer(caFile, "/etc/rabbitmq/ca_cert.pem")
                .withCopyFileToContainer(keyFile, "/etc/rabbitmq/rabbitmq_key.pem");
    }

    public S withPluginsEnabled(String... pluginNames) {
        List<String> command = new ArrayList<>(asList("rabbitmq-plugins", "enable"));
        command.addAll(asList(pluginNames));
        command.add("--offline");
        return self();
    }

    public S withPluginsDisabled(String... pluginNames) {
        List<String> command = new ArrayList<>(asList("rabbitmq-plugins", "disable"));
        command.addAll(asList(pluginNames));
        command.add("--offline");
        values.add(command);
        return self();
    }

    public S withBinding(String source, String destination) {
        values.add(asList("rabbitmqadmin", "declare", "binding", "source=" + source, "destination=" + destination));
        return self();
    }

    public S withBinding(String source, String destination, Map<String, Object> arguments, String routingKey, String destinationType) {
        values.add(asList("rabbitmqadmin", "declare", "binding", "source=" + source,
                "destination=" + destination, "routing-key=" + routingKey, "destination-type=" + destinationType,
                "arguments=" + toJson(arguments)));
        return self();
    }

    public S withParameter(String component, String name, String value) {
        values.add(asList("rabbitmqadmin", "declare", "parameter", "component=" + component, "name=" + name, "value=" + value));
        return self();
    }

    public S withPermission(String vhost, String user, String configure, String write, String read) {
        values.add(asList("rabbitmqadmin", "declare", "permission", "vhost=" + vhost, "user=" + user,
                "configure=" + configure, "write=" + write, "read=" + read));
        return self();
    }

    public S withUser(String name, String password) {
        values.add(asList("rabbitmqadmin", "declare", "user", "name=" + name, "password=" + password, "tags="));
        return self();
    }

    public S withUser(String name, String password, Set<String> tags) {
        values.add(asList("rabbitmqadmin", "declare", "user", "name=" + name, "password=" + password,
                "tags=" + join(",", tags)));
        return self();
    }

    public S withPolicy(String name, String pattern, Map<String, Object> definition) {
        values.add(asList("rabbitmqadmin", "declare", "policy", "name=" + name, "pattern=" + pattern,
                "definition=" + toJson(definition)));
        return self();
    }

    public S withPolicy(String name, String pattern, Map<String, Object> definition, int priority, String applyTo) {
        values.add(asList("rabbitmqadmin", "declare", "policy", "name=" + name,
                "pattern=" + pattern, "priority=" + priority, "apply-to=" + applyTo, "definition=" + toJson(definition)));
        return self();
    }

    public S withOperatorPolicy(String name, String pattern, Map<String, Object> definition) {
        values.add(new ArrayList<>(asList("rabbitmqadmin", "declare", "operator_policy", "name=" + name,
                "pattern=" + pattern, "definition=" + toJson(definition))));
        return self();
    }

    public S withOperatorPolicy(String name, String pattern, Map<String, Object> definition, int priority, String applyTo) {
        values.add(asList("rabbitmqadmin", "declare", "operator_policy", "name=" + name, "pattern=" + pattern,
                "priority=" + priority, "apply-to=" + applyTo, "definition=" + toJson(definition)));
        return self();
    }

    public S withVhost(String name) {
        values.add(asList("rabbitmqadmin", "declare", "vhost", "name=" + name));
        return self();
    }

    public S withVhost(String name, boolean tracing) {
        values.add(asList("rabbitmqadmin", "declare", "vhost", "name=" + name, "tracing=" + tracing));
        return self();
    }

    public S withVhostLimit(String vhost, String name, int value) {
        values.add(asList("rabbitmqadmin", "declare", "vhost_limit", "vhost=" + vhost, "name=" + name, "value=" + value));
        return self();
    }

    public S withQueue(String name) {
        values.add(asList("rabbitmqadmin", "declare", "queue", "name=" + name));
        return self();
    }

    public S withQueue(String name, boolean autoDelete, boolean durable, Map<String, Object> arguments) {
        values.add(asList("rabbitmqadmin", "declare", "queue", "name=" + name,
                "auto_delete=" + autoDelete, "durable=" + durable, "arguments=" + toJson(arguments)));

        return self();
    }

    @NotNull
    private JSONObject toJson(Map<String, Object> arguments) {
        JSONObject jsonObject = new JSONObject();
        arguments.forEach(jsonObject::put);
        return jsonObject;
    }

    public S withExchange(String name, String type) {
        values.add(asList("rabbitmqadmin", "declare", "exchange", "name=" + name, "type=" + type));
        return self();
    }

    public S withExchange(String name, String type, boolean autoDelete, boolean internal, boolean durable, Map<String, Object> arguments) {
        values.add(asList("rabbitmqadmin", "declare", "exchange", "name=" + name,
                "type=" + type, "auto_delete=" + autoDelete, "internal=" + internal, "durable=" + durable,
                "arguments=" + toJson(arguments)));
        return self();
    }

    /**
     * Overwrites the default RabbitMQ configuration file with the supplied one
     *
     * @param rabbitMQConf The rabbitmq.conf file to use
     * @return This container.
     */
    public S withRabbitMQConfig(MountableFile rabbitMQConf) {
        withEnv("RABBITMQ_CONFIG_FILE", "/etc/rabbitmq/rabbitmq-custom.conf");
        return withCopyFileToContainer(rabbitMQConf, "/etc/rabbitmq/rabbitmq-custom.conf");
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

}
