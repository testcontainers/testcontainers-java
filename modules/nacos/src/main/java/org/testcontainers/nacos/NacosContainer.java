package org.testcontainers.nacos;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Nacos.
 * <p>
 * Supported images: {@code nacos/nacos-server}, {@code nacos}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP: 8848</li>
 *     <li>HTTP: 8080</li>
 *     <li>gRPC: 9848</li>
 * </ul>
 *
 */
public class NacosContainer extends GenericContainer<NacosContainer> {

    private static final DockerImageName DEFAULT_OLD_IMAGE_NAME = DockerImageName.parse("nacos");

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("nacos/nacos-server");

    private static final int NACOS_HTTP_ADMIN_PORT = 8848;

    private static final int NACOS_HTTP_CONSOLE_PORT = 8080;

    private static final int NACOS_GRPC_PORT = 9848;

    public NacosContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName), 38848, 38080, 39848);
    }

    public NacosContainer(final DockerImageName dockerImageName, int adminPort, int consolePort, int grpcPort) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_OLD_IMAGE_NAME, DEFAULT_IMAGE_NAME);

        // Wait until the Nacos server is ready to accept requests.
        // Visit the login page to verify if nacos is running.
        setWaitStrategy(Wait.forHttp("/#/login").forPort(NACOS_HTTP_CONSOLE_PORT).forStatusCode(200));

        // According to Nacos' design, the gRPC client port adds 1000 to the main port, which means that if the main port is 8849, the gRPC port defaults to 9849
        addFixedExposedPort(adminPort, NACOS_HTTP_ADMIN_PORT);
        addFixedExposedPort(consolePort, NACOS_HTTP_CONSOLE_PORT);
        addFixedExposedPort(grpcPort, NACOS_GRPC_PORT);

        // Configure Nacos for single machine startup.
        withEnv("MODE", "standalone");
        // Nacos is used to generate keys for JWT tokens, using strings longer than 32 characters and then encoded with Base64.
        withEnv("NACOS_AUTH_TOKEN", "SecretKey012345678901234567890123456789012345678901234567890123456789");
        // The key for the identity identifier of the Inner API between Nacos servers is required.
        withEnv("NACOS_AUTH_IDENTITY_KEY", "serverIdentity");
        // The value of the identity identifier for the Inner API between Nacos servers is required.
        withEnv("NACOS_AUTH_IDENTITY_VALUE", "security");
    }

    public String getServerAddr() {
        return String.format("%s:%s", this.getHost(), this.getMappedPort(NACOS_HTTP_ADMIN_PORT));
    }

}
