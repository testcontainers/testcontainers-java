package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class WaitStrategiesTest {

    @Rule
    // waitForNetworkListening {
    public GenericContainer nginx = new GenericContainer(DockerImageName.parse("nginx:1.9.4")) //
        .withExposedPorts(80);

    // }

    @Rule
    // waitForSimpleHttp {
    public GenericContainer nginxWithHttpWait = new GenericContainer(DockerImageName.parse("nginx:1.9.4"))
        .withExposedPorts(80)
        .waitingFor(Wait.forHttp("/"));

    // }

    @Rule
    // logMessageWait {
    public GenericContainer containerWithLogWait = new GenericContainer(DockerImageName.parse("redis:6-alpine"))
        .withExposedPorts(6379)
        .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));

    // }

    private static final HttpWaitStrategy MULTI_CODE_HTTP_WAIT =
        // spotless:off
        // waitForHttpWithMultipleStatusCodes {
        Wait.forHttp("/")
            .forStatusCode(200)
            .forStatusCode(301);
        // }
        // spotless:on

    private static final HttpWaitStrategy PREDICATE_HTTP_WAIT =
        // spotless:off
        // waitForHttpWithStatusCodePredicate {
        Wait.forHttp("/all")
            .forStatusCodeMatching(it -> it >= 200 && it < 300 || it == 401);
        // }
        // spotless:on

    private static final HttpWaitStrategy TLS_HTTP_WAIT =
        // spotless:off
        // waitForHttpWithTls {
        Wait.forHttp("/all")
            .usingTls();
        // }
        // spotless:on

    private static final WaitStrategy HEALTHCHECK_WAIT =
        // spotless:off
        // healthcheckWait {
        Wait.forHealthcheck();
        // }
        // spotless:on

    @Test
    public void testContainersAllStarted() {
        assertThat(nginx.isRunning()).isTrue();
        assertThat(nginxWithHttpWait.isRunning()).isTrue();
        assertThat(containerWithLogWait.isRunning()).isTrue();
    }
}
