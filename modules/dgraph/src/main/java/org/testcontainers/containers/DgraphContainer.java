package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.stream.Collectors.toSet;

/**
 * Testcontainer for Dgraph.
 *
 * @param <S> "SELF" to be used in the <code>withXXX</code> methods.
 * @author Enrico Minack
 */
public class DgraphContainer<S extends DgraphContainer<S>> extends GenericContainer<S> {

    /**
     * The image defaults to the official Dgraph image: <a href="https://hub.docker.com/_/dgraph/dgraph">Dgraph</a>.
     */
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("dgraph/dgraph");

    private static final int HTTP_PORT = 8080;

    private static final int GRPC_PORT = 9080;

    /**
     * Creates a DgraphContainer using a specific docker image and a startup timeout of 1 minute.
     *
     * @param dockerImageName The docker image to use.
     */
    public DgraphContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName), Duration.ofMinutes(1));
    }

    /**
     * Creates a DgraphContainer using a specific docker image and a startup timeout of 1 minute.
     *
     * @param dockerImageName The docker image to use.
     */
    public DgraphContainer(@NonNull final DockerImageName dockerImageName) {
        this(dockerImageName, Duration.ofMinutes(1));
    }

    /**
     * Creates a DgraphContainer using a specific docker image. Connect the container
     * to another DgraphContainer to form a cluster via `peerAlias`.
     *
     * @param dockerImageName The docker image to use.
     * @param startupTimeout Timeout for the container startup.
     */
    public DgraphContainer(@NonNull final DockerImageName dockerImageName,
                           @NonNull Duration startupTimeout) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        WaitStrategy waitForCluster = new LogMessageWaitStrategy()
            .withRegEx(".* Server is ready\n");
        WaitStrategy waitForHttp = new HttpWaitStrategy()
            .forPort(HTTP_PORT)
            .forStatusCodeMatching(response -> response == HTTP_OK);

        this.waitStrategy = new WaitAllStrategy()
            .withStrategy(waitForCluster)
            .withStrategy(waitForHttp)
            .withStartupTimeout(startupTimeout);

        String whitelist;
        if (dockerImageName.getVersionPart().compareTo("v21.03.0") < 0)
            whitelist = "--whitelist 0.0.0.0/0";
        else
            whitelist = "--security whitelist=0.0.0.0/0";

        this.setCommand("/bin/bash", "-c", "dgraph zero & dgraph alpha " + whitelist + " --zero localhost:5080");

        addExposedPorts(HTTP_PORT, GRPC_PORT);
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return Stream.of(getHttpPort(), getGrpcPort())
            .map(this::getMappedPort)
            .collect(toSet());
    }

    @Override
    protected void configure() { }

    public int getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    public int getGrpcPort() {
        return getMappedPort(GRPC_PORT);
    }

    public String getHttpUrl() {
        return String.format("http://%s:%d", getHost(), getHttpPort());
    }

    public String getGrpcUrl() {
        return String.format("%s:%d", getHost(), getGrpcPort());
    }

}
