package org.testcontainers.containers;

import org.assertj.core.api.Assumptions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@RunWith(Parameterized.class)
public class DockerComposeOverridesTest {

    private static final File BASE_COMPOSE_FILE = new File("src/test/resources/docker-compose-base.yml");

    private static final String BASE_ENV_VAR = "bar=base";

    private static final File OVERRIDE_COMPOSE_FILE = new File(
        "src/test/resources/docker-compose-non-default-override.yml"
    );

    private static final String OVERRIDE_ENV_VAR = "bar=overwritten";

    private static final int SERVICE_PORT = 3000;

    private static final String SERVICE_NAME = "alpine_1";

    private final boolean localMode;

    private final String expectedEnvVar;

    private final File[] composeFiles;

    public DockerComposeOverridesTest(boolean localMode, String expectedEnvVar, File... composeFiles) {
        this.localMode = localMode;
        this.expectedEnvVar = expectedEnvVar;
        this.composeFiles = composeFiles;
    }

    @Parameters(name = "{index}: local[{0}], composeFiles[{2}], expectedEnvVar[{1}]")
    public static Iterable<Object[]> data() {
        return Arrays.asList(
            new Object[][] {
                { true, BASE_ENV_VAR, new File[] { BASE_COMPOSE_FILE } },
                { true, OVERRIDE_ENV_VAR, new File[] { BASE_COMPOSE_FILE, OVERRIDE_COMPOSE_FILE } },
                { false, BASE_ENV_VAR, new File[] { BASE_COMPOSE_FILE } },
                { false, OVERRIDE_ENV_VAR, new File[] { BASE_COMPOSE_FILE, OVERRIDE_COMPOSE_FILE } },
            }
        );
    }

    @Before
    public void setUp() {
        if (localMode) {
            Assumptions
                .assumeThat(LocalDockerCompose.executableExists())
                .as("docker-compose executable exists")
                .isTrue();
        }
    }

    @Test
    public void test() {
        try (
            DockerComposeContainer compose = new DockerComposeContainer(composeFiles)
                .withLocalCompose(localMode)
                .withExposedService(SERVICE_NAME, SERVICE_PORT)
        ) {
            compose.start();

            BufferedReader br = await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(
                    () -> {
                        Socket socket = new Socket(
                            compose.getServiceHost(SERVICE_NAME, SERVICE_PORT),
                            compose.getServicePort(SERVICE_NAME, SERVICE_PORT)
                        );
                        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    },
                    Objects::nonNull
                );

            await()
                .atMost(10, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .until(() -> {
                    while (br.ready()) {
                        String line = br.readLine();
                        if (line.contains(expectedEnvVar)) {
                            return true;
                        }
                    }
                    return false;
                });
        }
    }
}
