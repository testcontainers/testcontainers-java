package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ExecTest {

    @Rule
    public TestcontainersRule<GenericContainer<?>> container = new TestcontainersRule<>(
        new GenericContainer<>(DockerImageName.parse("alpine:3.17")).withCommand("top")
    );

    @Test
    public void testSimpleExec() throws IOException, InterruptedException {
        // standaloneExec {
        container.get().execInContainer("touch", "/somefile.txt");
        // }

        // execReadingStdout {
        Container.ExecResult lsResult = container.get().execInContainer("ls", "-al", "/");
        String stdout = lsResult.getStdout();
        int exitCode = lsResult.getExitCode();
        assertThat(stdout).contains("somefile.txt");
        assertThat(exitCode).isZero();
        // }
    }
}
