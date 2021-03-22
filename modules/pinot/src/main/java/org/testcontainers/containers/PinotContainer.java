package org.testcontainers.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.utility.DockerImageName;

public class PinotContainer extends GenericContainer<PinotContainer> {

    public static final int BROKER_HTTP_PORT = 9000;
    public static final String DEFAULT_ENDPOINT = "/";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("apachepinot/pinot");
    private static final String DEFAULT_TAG = "latest";

    public PinotContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public PinotContainer(String pulsarVersion) {
        this(DEFAULT_IMAGE_NAME.withTag(pulsarVersion));
    }

    public PinotContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DockerImageName.parse("apachepinot/pinot"));
        withExposedPorts(BROKER_HTTP_PORT);
        withCommand("QuickStart", "-type", "batch");
        waitingFor(Wait.forHttp(DEFAULT_ENDPOINT).forStatusCode(200).forPort(BROKER_HTTP_PORT));
    }

    @Override
    protected void configure() {
        super.configure();
        withCreateContainerCmdModifier(createContainerCmd -> createContainerCmd.withName("pinot-quickstart"));
        withCommand("QuickStart", "-type", "batch");
        waitingFor(
            new WaitAllStrategy()
                    .withStrategy(waitStrategy)
                    .withStrategy(Wait.forListeningPort()));
    }

    public String getPinotBrokerUrl() {
        return String.format("http://%s:%s", getHost(), getBrokerPort());
    }

    public Integer getBrokerPort() {
        return getMappedPort(BROKER_HTTP_PORT);
    }
}
