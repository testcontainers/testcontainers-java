package org.testcontainers.junit;

import com.google.common.util.concurrent.Uninterruptibles;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.TestEnvironment;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import static org.rnorth.visibleassertions.VisibleAssertions.info;
import static org.rnorth.visibleassertions.VisibleAssertions.pass;
import static org.testcontainers.junit.DockerComposeDoNotOverrideTest.DOCKER_COMPOSE_OVERRIDE_TEST_BASE_YML;
import static org.testcontainers.junit.DockerComposeDoNotOverrideTest.DOCKER_COMPOSE_OVERRIDE_TEST_OVERRIDE_ENV;
import static org.testcontainers.junit.DockerComposeDoNotOverrideTest.DOCKER_COMPOSE_OVERRIDE_TEST_OVERRIDE_YML;

/**
 * Created by rnorth on 11/06/2016.
 */
public class DockerComposeLocalOverrideTest {

    private static final int SERVICE_PORT = 3000;
    private static final String SERVICE_NAME = "alpine_1";

    @Rule
    public DockerComposeContainer compose =
            new DockerComposeContainer(
                    new File(DOCKER_COMPOSE_OVERRIDE_TEST_BASE_YML),
                    new File(DOCKER_COMPOSE_OVERRIDE_TEST_OVERRIDE_YML))
                    .withExposedService(SERVICE_NAME, SERVICE_PORT)
                    .withLocalCompose(true);

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Test(timeout = 30_000)
    public void testEnvVar() {
        BufferedReader reader = Unreliables.retryUntilSuccess(10, TimeUnit.SECONDS, () -> {
            Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

            Socket socket = new Socket(
                compose.getServiceHost(SERVICE_NAME, SERVICE_PORT),
                compose.getServicePort(SERVICE_NAME, SERVICE_PORT));
            return new BufferedReader(new InputStreamReader(socket.getInputStream()));
        });

        Unreliables.retryUntilTrue(10, TimeUnit.SECONDS, () -> {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line.contains(DOCKER_COMPOSE_OVERRIDE_TEST_OVERRIDE_ENV)) {
                    pass("Mapped environment variable was found");
                    return true;
                }
            }
            info("Mapped environment variable was not found yet - process probably not ready");
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
            return false;
        });

    }
}
