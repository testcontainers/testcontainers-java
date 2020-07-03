package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;
import org.testcontainers.utility.DockerImageName;

import static org.junit.Assert.assertTrue;

public class WaitStrategiesTest {

    @Rule
    // waitForNetworkListening {
    public GenericContainer nginx = new GenericContainer(DockerImageName.parse("nginx:1.9.4"))
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
    public GenericContainer containerWithLogWait = new GenericContainer(DockerImageName.parse("redis:5.0.3"))
        .withExposedPorts(6379)
        .waitingFor(
            Wait.forLogMessage(".*Ready to accept connections.*\\n", 1)
        );
    // }

    private static final HttpWaitStrategy MULTI_CODE_HTTP_WAIT =
        // waitForHttpWithMultipleStatusCodes {
        Wait.forHttp("/")
            .forStatusCode(200)
            .forStatusCode(301)
        // }
        ;

    private static final HttpWaitStrategy PREDICATE_HTTP_WAIT =
        // waitForHttpWithStatusCodePredicate {
        Wait.forHttp("/all")
            .forStatusCodeMatching(it -> it >= 200 && it < 300 || it == 401)
        // }
        ;

    private static final HttpWaitStrategy TLS_HTTP_WAIT =
        // waitForHttpWithTls {
        Wait.forHttp("/all")
            .usingTls()
        // }
        ;

    private static final WaitStrategy HEALTHCHECK_WAIT =
        // healthcheckWait {
        Wait.forHealthcheck()
        // }
        ;

    @Test
    public void testContainersAllStarted() {
        assertTrue(nginx.isRunning());
        assertTrue(nginxWithHttpWait.isRunning());
        assertTrue(containerWithLogWait.isRunning());
    }
}
