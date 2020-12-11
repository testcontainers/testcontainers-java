package org.testcontainers.containers;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
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

    private static final String ORIGINAL_RECORDING_FILE_NAME = "/screen.flv";

    public static final String DEFAULT_VNC_PASSWORD = "secret";

    public static final int DEFAULT_VNC_PORT = 5900;

    private final String targetNetworkAlias;

    private String vncPassword = DEFAULT_VNC_PASSWORD;

    private VncRecordingFormat recordingFormat = VncRecordingFormat.FLV;

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
        super(DockerImageName.parse("testcontainers/vnc-recorder:1.2.0"));

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

    public VncRecordingContainer withVideoExtension(VncRecordingFormat recordingFormat) {
        if (recordingFormat != null) {
            this.recordingFormat = recordingFormat;
        }
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
                        "flvrec.py -o " + ORIGINAL_RECORDING_FILE_NAME + " -d -r " + frameRate + " -P /vnc_password " + targetNetworkAlias + " " + vncPort
        );
    }

    @SneakyThrows
    public void saveRecordingToFile(File file) {
        try(InputStream inputStream = streamRecording()) {
            Files.copy(inputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SneakyThrows
    public InputStream streamRecording() {
        String newRecordingFileName = recordingFormat.reencodeRecording(this, ORIGINAL_RECORDING_FILE_NAME);

        TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(getContainerId(), newRecordingFileName).exec()
        );
        archiveInputStream.getNextEntry();
        return archiveInputStream;
    }


    public enum VncRecordingFormat {
        FLV("flv") {
            @Override
            String reencodeRecording(VncRecordingContainer container, String source) throws IOException, InterruptedException {
                String newFileOutput = "/newScreen.flv";
                container.execInContainer("ffmpeg" , "-i", source, "-vcodec", "libx264", newFileOutput);
                return newFileOutput;
            }
        },
        MP4("mp4") {
            @Override
            String reencodeRecording(VncRecordingContainer container, String source) throws IOException, InterruptedException {
                String newFileOutput = "/newScreen.mp4";
                container.execInContainer("ffmpeg" , "-i", source, "-vcodec", "libx264", "-movflags", "faststart", newFileOutput);
                return newFileOutput;
            }
        };

        abstract String reencodeRecording(VncRecordingContainer container, String source) throws IOException, InterruptedException;

        private final String filenameExtension;

        VncRecordingFormat(String filenameExtension) {
            this.filenameExtension = filenameExtension;
        }

        public String getExtension() {
            return filenameExtension;
        }
    }
}
