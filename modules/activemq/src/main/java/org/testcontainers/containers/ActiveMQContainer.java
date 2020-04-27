package org.testcontainers.containers;

import static java.lang.String.join;
import static java.util.Arrays.asList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainer for ActiveMQ.
 *
 * @author Isaac A. Nugroho
 */
public class ActiveMQContainer extends GenericContainer<ActiveMQContainer> {

    /**
     * The image defaults to rmohr's ActiveMQ image: <a href="https://registry.hub.docker.com/u/rmohr/activemq/">ActiveMQ</a>.
     */
    private static final String DEFAULT_IMAGE_NAME = "rmohr/activemq";
    private static final String DEFAULT_TAG = "5.15.9-alpine";

    private static final int DEFAULT_STOMP_PORT = 61613;
    private static final int DEFAULT_WS_PORT = 61614;
    private static final int DEFAULT_JMS_PORT = 61616;
    private static final int DEFAULT_MQTT_PORT = 1883;
    private static final int DEFAULT_AMQP_PORT = 5672;
    private static final int DEFAULT_UI_PORT = 8161;

    private static final String DEFAULT_CONNECTION_CONFIGURATION = "?maximumConnections=1000&wireFormat.maxFrameSize=104857600";
    private static final String ACTIVEMQ_CONF_DIR = "/opt/activemq/conf/";

    /**
     * Creates a Testcontainer using the official ActiveMQ docker image.
     */
    public ActiveMQContainer() {
        this(DEFAULT_IMAGE_NAME + ":" + DEFAULT_TAG);
    }

    /**
     * Creates a Testcontainer using a specific docker image.
     *
     * @param image The docker image to use.
     */
    public ActiveMQContainer(String image) {
        super(image);

        addExposedPorts(DEFAULT_STOMP_PORT, DEFAULT_WS_PORT, DEFAULT_JMS_PORT, DEFAULT_MQTT_PORT,
            DEFAULT_AMQP_PORT, DEFAULT_UI_PORT);

        this.waitStrategy = Wait.
            forLogMessage(".*Apache ActiveMQ.*started.*", 1)
            .withStartupTimeout(Duration.ofSeconds(60));
    }

    public Integer getStompPort() {
        return getMappedPort(DEFAULT_STOMP_PORT);
    }

    public Integer getWsPort() {
        return getMappedPort(DEFAULT_WS_PORT);
    }

    public Integer getJmsPort() {
        return getMappedPort(DEFAULT_JMS_PORT);
    }

    public Integer getMqttPort() {
        return getMappedPort(DEFAULT_MQTT_PORT);
    }

    public Integer getAmqpPort() {
        return getMappedPort(DEFAULT_AMQP_PORT);
    }

    public Integer getUiPort() {
        return getMappedPort(DEFAULT_UI_PORT);
    }

    /**
     * @return STOMP URL for use with STOMP clients.
     */
    public String getStompUrl() {
        return "stomp://" + getContainerIpAddress() + ":" + getStompPort() + DEFAULT_CONNECTION_CONFIGURATION;
    }

    /**
     * @return WebSocket URL for use with WebSocket clients.
     */
    public String getWsUrl() {
        return "ws://" + getContainerIpAddress() + ":" + getWsPort() + DEFAULT_CONNECTION_CONFIGURATION;
    }

    /**
     * @return JMS URL for use with JMS clients.
     */
    public String getJmsUrl() {
        return "tcp://" + getContainerIpAddress() + ":" + getJmsPort() + DEFAULT_CONNECTION_CONFIGURATION;
    }

    /**
     * @return MQTT URL for use with MQTT clients.
     */
    public String getMqttUrl() {
        return "mqtt://" + getContainerIpAddress() + ":" + getMqttPort() + DEFAULT_CONNECTION_CONFIGURATION;
    }

    /**
     * @return MQTT URL for use with MQTT clients.
     */
    public String getAmqpUrl() {
        return "amqp://" + getContainerIpAddress() + ":" + getAmqpPort() + DEFAULT_CONNECTION_CONFIGURATION;
    }

    /**
     * @return URL of the WebConsole endpoint.
     */
    public String getUiUrl() {
        return "http://" + getContainerIpAddress() + ":" + getUiPort();
    }

    /**
     * Overwrites the default ActiveMQ configuration files with the supplied one.
     *
     * @param activeMQConf The config file to use
     * @return This container.
     */
    public ActiveMQContainer withActiveMQConfigFile(MountableFile activeMQConf) {
        return withCopyFileToContainer(activeMQConf, ACTIVEMQ_CONF_DIR);
    }
}
