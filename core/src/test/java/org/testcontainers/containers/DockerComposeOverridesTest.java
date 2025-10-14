package org.testcontainers.containers;

import com.google.common.util.concurrent.Uninterruptibles;
import org.assertj.core.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.utility.CommandLine;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class DockerComposeOverridesTest {

    private static final File BASE_COMPOSE_FILE = new File("src/test/resources/docker-compose-base.yml");

    private static final String BASE_ENV_VAR = "bar=base";

    private static final File OVERRIDE_COMPOSE_FILE = new File(
        "src/test/resources/docker-compose-non-default-override.yml"
    );

    private static final String OVERRIDE_ENV_VAR = "bar=overwritten";

    private static final int SERVICE_PORT = 3000;

    private static final String SERVICE_NAME = "alpine_1";

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

    @ParameterizedTest(name = "{index}: local[{0}], composeFiles[{2}], expectedEnvVar[{1}]")
    @MethodSource("data")
    void test(boolean localMode, String expectedEnvVar, File... composeFiles) {
        DockerComposeContainer compose;
        if (localMode) {
            Assumptions
                .assumeThat(CommandLine.executableExists(DockerComposeContainer.COMPOSE_EXECUTABLE))
                .as("docker-compose executable exists")
                .isTrue();
            Assumptions
                .assumeThat(CommandLine.runShellCommand("docker-compose", "--version"))
                .doesNotStartWith("Docker Compose version v2");

            compose = new DockerComposeContainer(composeFiles).withExposedService(SERVICE_NAME, SERVICE_PORT);
        } else {
            compose =
                new DockerComposeContainer(DockerImageName.parse("docker/compose:debian-1.29.2"), composeFiles)
                    .withExposedService(SERVICE_NAME, SERVICE_PORT);
        }

        compose.start();

        BufferedReader br = Unreliables.retryUntilSuccess(
            10,
            TimeUnit.SECONDS,
            () -> {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);

                Socket socket = new Socket(
                    compose.getServiceHost(SERVICE_NAME, SERVICE_PORT),
                    compose.getServicePort(SERVICE_NAME, SERVICE_PORT)
                );
                return new BufferedReader(new InputStreamReader(socket.getInputStream()));
            }
        );

        Unreliables.retryUntilTrue(
            10,
            TimeUnit.SECONDS,
            () -> {
                while (br.ready()) {
                    String line = br.readLine();
                    if (line.contains(expectedEnvVar)) {
                        return true;
                    }
                }
                Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
                return false;
            }
        );
        compose.stop();
    }
}
