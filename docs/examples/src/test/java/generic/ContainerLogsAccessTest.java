package generic;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;

public class ContainerLogsAccessTest {

    @Rule
    public GenericContainer container = new GenericContainer<>("redis:3.0.2");

    @Test
    public void testGetAllLogs() {
        final String logs = container.getLogs();
    }

    @Test
    public void testGetStdout() {
        final String logs = container.getLogs(OutputFrame.OutputType.STDOUT);
    }

    @Test
    public void testGetStderr() {
        final String logs = container.getLogs(OutputFrame.OutputType.STDERR);
    }
}
