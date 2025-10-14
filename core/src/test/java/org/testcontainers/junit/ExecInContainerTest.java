package org.testcontainers.junit;

import org.assertj.core.api.Assumptions;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.TestEnvironment;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class ExecInContainerTest {

    @AutoClose
    public static GenericContainer<?> redis = new GenericContainer<>(TestImages.REDIS_IMAGE).withExposedPorts(6379);

    static {
        redis.start();
    }

    @Test
    void shouldExecuteCommand() throws Exception {
        // The older "lxc" execution driver doesn't support "exec". At the time of writing (2016/03/29),
        // that's the case for CircleCI.
        // Once they resolve the issue, this clause can be removed.
        Assumptions.assumeThat(TestEnvironment.dockerExecutionDriverSupportsExec()).isTrue();

        final GenericContainer.ExecResult result = redis.execInContainer("redis-cli", "role");
        assertThat(result.getStdout())
            .as("Output for \"redis-cli role\" command should start with \"master\"")
            .startsWith("master");
        assertThat(result.getStderr()).as("Stderr for \"redis-cli role\" command should be empty").isEmpty();
        // We expect to reach this point for modern Docker versions.
    }

    @Test
    void shouldExecuteCommandWithUser() throws Exception {
        // The older "lxc" execution driver doesn't support "exec". At the time of writing (2016/03/29),
        // that's the case for CircleCI.
        // Once they resolve the issue, this clause can be removed.
        Assumptions.assumeThat(TestEnvironment.dockerExecutionDriverSupportsExec()).isTrue();

        final GenericContainer.ExecResult result = redis.execInContainerWithUser("redis", "whoami");
        assertThat(result.getStdout())
            .as("Output for \"whoami\" command should start with \"redis\"")
            .startsWith("redis");
        assertThat(result.getStderr()).as("Stderr for \"whoami\" command should be empty").isEmpty();
        // We expect to reach this point for modern Docker versions.
    }

    @Test
    void shouldExecuteCommandWithWorkdir() throws Exception {
        Assumptions.assumeThat(TestEnvironment.dockerExecutionDriverSupportsExec()).isTrue();

        final GenericContainer.ExecResult result = redis.execInContainer(
            ExecConfig.builder().workDir("/opt").command(new String[] { "pwd" }).build()
        );
        assertThat(result.getStdout()).startsWith("/opt");
    }

    @Test
    void shouldExecuteCommandWithEnvVars() throws Exception {
        Assumptions.assumeThat(TestEnvironment.dockerExecutionDriverSupportsExec()).isTrue();

        final GenericContainer.ExecResult result = redis.execInContainer(
            ExecConfig
                .builder()
                .envVars(Collections.singletonMap("TESTCONTAINERS", "JAVA"))
                .command(new String[] { "env" })
                .build()
        );
        assertThat(result.getStdout()).contains("TESTCONTAINERS=JAVA");
    }
}
