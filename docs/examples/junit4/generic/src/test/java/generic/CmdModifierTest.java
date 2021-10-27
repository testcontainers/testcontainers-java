package generic;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.assertEquals;

public class CmdModifierTest {

    // hostname {
    @Rule
    public GenericContainer theCache = new GenericContainer<>(DockerImageName.parse("redis:3.0.2"))
            .withCreateContainerCmdModifier(cmd -> cmd.withHostName("the-cache"));
    // }

    // memory {
    private long memoryInBytes = 32 * 1024 * 1024;
    private long memorySwapInBytes = 64 * 1024 * 1024;

    @Rule
    public GenericContainer memoryLimitedRedis = new GenericContainer<>(DockerImageName.parse("redis:3.0.2"))
            .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                .withMemory(memoryInBytes)
                .withMemorySwap(memorySwapInBytes)
            );
    // }


    @Test
    public void testHostnameModified() throws IOException, InterruptedException {
        final Container.ExecResult execResult = theCache.execInContainer("hostname");
        assertEquals("the-cache", execResult.getStdout().trim());
    }

    @Test
    public void testMemoryLimitModified() throws IOException, InterruptedException {
        final Container.ExecResult execResult = memoryLimitedRedis.execInContainer("cat", getMemoryLimitFilePath());
        assertEquals(String.valueOf(memoryInBytes), execResult.getStdout().trim());
    }

    private String getMemoryLimitFilePath() {
        DockerClient dockerClient = DockerClientFactory.instance().client();
        Info info = dockerClient.infoCmd().exec();
        Object cgroupVersion = info.getRawValues().get("CgroupVersion");
        boolean cgroup2 = Objects.equals("2", cgroupVersion);
        if (cgroup2) {
            return "/sys/fs/cgroup/memory.max";
        }
        return "/sys/fs/cgroup/memory/memory.limit_in_bytes";
    }
}
