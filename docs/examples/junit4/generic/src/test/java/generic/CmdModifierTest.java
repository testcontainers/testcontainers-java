package generic;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class CmdModifierTest {

    // hostname {
    @Container
    public GenericContainer theCache = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
        .withCreateContainerCmdModifier(cmd -> cmd.withHostName("the-cache"));

    // }

    // spotless:off
    // memory {
    private long memoryInBytes = 32l * 1024l * 1024l;

    private long memorySwapInBytes = 64l * 1024l * 1024l;

    @Container
    public GenericContainer memoryLimitedRedis = new GenericContainer<>(DockerImageName.parse("redis:6-alpine"))
        .withCreateContainerCmdModifier(cmd -> {
            cmd.getHostConfig()
                .withMemory(memoryInBytes)
                .withMemorySwap(memorySwapInBytes);
        });

    // }
    // spotless:on

    @Test
    public void testHostnameModified() throws IOException, InterruptedException {
        final ExecResult execResult = theCache.execInContainer("hostname");
        assertThat(execResult.getStdout().trim()).isEqualTo("the-cache");
    }

    @Test
    public void testMemoryLimitModified() throws IOException, InterruptedException {
        final ExecResult execResult = memoryLimitedRedis.execInContainer("cat", getMemoryLimitFilePath());
        assertThat(execResult.getStdout().trim()).isEqualTo(String.valueOf(memoryInBytes));
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
