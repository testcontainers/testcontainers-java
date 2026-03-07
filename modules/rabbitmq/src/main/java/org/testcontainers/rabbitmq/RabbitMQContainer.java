package org.testcontainers.rabbitmq;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Testcontainers implementation for RabbitMQ.
 * <p>
 * Supported image: {@code rabbitmq}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>5671 (AMQPS)</li>
 *     <li>5672 (AMQP)</li>
 *     <li>15671 (HTTPS)</li>
 *     <li>15672 (HTTP)</li>
 * </ul>
 */
public class RabbitMQContainer extends GenericContainer<RabbitMQContainer> {

    /**
     * The image defaults to the official RabbitMQ image: <a href="https://hub.docker.com/_/rabbitmq/">RabbitMQ</a>.
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("rabbitmq");

    private static final int DEFAULT_AMQP_PORT = 5672;

    private static final int DEFAULT_AMQPS_PORT = 5671;

    private static final int DEFAULT_HTTPS_PORT = 15671;

    private static final int DEFAULT_HTTP_PORT = 15672;

    private String adminPassword = "guest";

    private String adminUsername = "guest";

    private final List<List<String>> values = new ArrayList<>();

    /**
     * Creates a RabbitMQ container using a specific docker image.
     *
     * @param dockerImageName The docker image to use.
     */
    public RabbitMQContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public RabbitMQContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        addExposedPorts(DEFAULT_AMQP_PORT, DEFAULT_AMQPS_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT);

        waitingFor(Wait.forLogMessage(".*Server startup complete.*", 1));
    }

    @Override
    protected void configure() {
        if (this.adminUsername != null) {
            addEnv("RABBITMQ_DEFAULT_USER", this.adminUsername);
        }
        if (this.adminPassword != null) {
            addEnv("RABBITMQ_DEFAULT_PASS", this.adminPassword);
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
        return this.adminPassword;
    }

    /**
     * @return The admin user for the <code>admin</code> account
     */
    public String getAdminUsername() {
        return this.adminUsername;
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
     * Sets the user for the admin (default is <pre>guest</pre>)
     *
     * @param adminUsername The admin user.
     * @return This container.
     */
    public RabbitMQContainer withAdminUser(final String adminUsername) {
        this.adminUsername = adminUsername;
        return this;
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
        withEnv("RABBITMQ_CONFIG_FILE", "/etc/rabbitmq/rabbitmq-custom.conf");
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
