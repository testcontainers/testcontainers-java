package org.testcontainers.containers;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Base64;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 * 'Sidekick container' with the sole purpose of recording the VNC screen output from another container.
 *
 */
@Getter
@ToString
public class VncRecordingContainer extends GenericContainer<VncRecordingContainer> {

    private static final String RECORDING_FILE_NAME = "/screen.flv";

    public static final String DEFAULT_VNC_PASSWORD = "secret";

    public static final int DEFAULT_VNC_PORT = 5900;

    private final String targetNetworkAlias;

    private String vncPassword = DEFAULT_VNC_PASSWORD;

    private int vncPort = 5900;

    private int frameRate = 30;

    public VncRecordingContainer(@NonNull GenericContainer<?> targetContainer) {
        this(
                targetContainer.getNetwork(),
                targetContainer.getNetworkAliases().stream()
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Target container must have a network alias"))
        );
    }

    /**
     * Create a sidekick container and attach it to another container. The VNC output of that container will be recorded.
     */
    public VncRecordingContainer(@NonNull Network network, @NonNull String targetNetworkAlias) throws IllegalStateException {
        super(TestcontainersConfiguration.getInstance().getVncDockerImageName());

        this.targetNetworkAlias = targetNetworkAlias;
        withNetwork(network);
        waitingFor(new LogMessageWaitStrategy()
            .withRegEx(".*Connected.*")
            .withStartupTimeout(Duration.of(15, SECONDS)));
    }

    public VncRecordingContainer withVncPassword(@NonNull String vncPassword) {
        this.vncPassword = vncPassword;
        return this;
    }

    public VncRecordingContainer withVncPort(int vncPort) {
        this.vncPort = vncPort;
        return this;
    }

    public VncRecordingContainer withFrameRate(int frameRate) {
        this.frameRate = frameRate;
        return this;
    }

    @Override
    protected void configure() {
        withCreateContainerCmdModifier(it -> it.withEntrypoint("/bin/sh"));
        String encodedPassword = Base64.getEncoder().encodeToString(vncPassword.getBytes());
        setCommand(
                "-c",
                "echo '" + encodedPassword + "' | base64 -d > /vnc_password && " +
                        "flvrec.py -o " + RECORDING_FILE_NAME + " -d -r " + frameRate + " -P /vnc_password " + targetNetworkAlias + " " + vncPort
        );
    }

    @SneakyThrows
    public InputStream streamRecording() {
        TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(getContainerId(), RECORDING_FILE_NAME).exec()
        );
        archiveInputStream.getNextEntry();
        return archiveInputStream;
    }

    @SneakyThrows
    public void saveRecordingToFile(File file) {
        try(InputStream inputStream = streamRecording()) {
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
