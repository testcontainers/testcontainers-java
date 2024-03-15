package org.testcontainers.openfga;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for OpenFGA.
 * <p>
 * Supported image: {@code openfga/openfga}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Playground: 3000</li>
 *     <li>HTTP: 8080</li>
 *     <li>gRPC: 8081</li>
 * </ul>
 */
public class OpenFGAContainer extends GenericContainer<OpenFGAContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("openfga/openfga");

    public OpenFGAContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public OpenFGAContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(3000, 8080, 8081);
        withCommand("run");
        waitingFor(
            Wait.forHttp("/healthz").forPort(8080).forResponsePredicate(response -> response.contains("SERVING"))
        );
    }

    public String getHttpEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(8080);
    }

    public String getGrpcEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(8081);
    }
}
