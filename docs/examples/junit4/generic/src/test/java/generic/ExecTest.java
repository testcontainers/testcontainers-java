package generic;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class ExecTest {

    @Container
    public GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse("alpine:3.17"))
        .withCommand("top");

    @Test
    public void testSimpleExec() throws IOException, InterruptedException {
        // standaloneExec {
        container.execInContainer("touch", "/somefile.txt");
        // }

        // execReadingStdout {
        ExecResult lsResult = container.execInContainer("ls", "-al", "/");
        String stdout = lsResult.getStdout();
        int exitCode = lsResult.getExitCode();
        assertThat(stdout).contains("somefile.txt");
        assertThat(exitCode).isZero();
        // }
    }
}
