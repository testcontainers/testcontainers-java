package org.testcontainers.containers;

import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerInfo;
import com.spotify.docker.client.messages.HostConfig;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.containers.traits.VncService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * 'Sidekick container' with the sole purpose of recording the VNC screen output from another container.
 */
public class VncRecordingSidekickContainer<T extends VncService & LinkableContainer> extends GenericContainer {
    private final T vncServiceContainer;
    private final Path tempDir;

    /**
     * Create a sidekick container and link it to another container. The VNC output of that container will be recorded.
     *
     * @param vncServiceContainer the container whose screen should be recorded. This container must implement VncService and LinkableContainer.
     */
    public VncRecordingSidekickContainer(T vncServiceContainer) {
        super("richnorth/vnc-recorder:latest");

        this.vncServiceContainer = vncServiceContainer;

        try {
            // Use a temporary volume directory that the container should record a file into
            this.tempDir = createVolumeDirectory(true);

            // flvrec.py needs to read the VNC password from a file
            Path passwordFile = tempDir.resolve("password");
            Files.write(passwordFile, vncServiceContainer.getPassword().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected void containerIsStarting(ContainerInfo containerInfo) {
        // do nothing
    }

    @Override
    protected String getLivenessCheckPort() {
        // no liveness check needed
        return null;
    }

    @Override
    protected ContainerConfig getContainerConfig() {
        return ContainerConfig.builder()
                .image(getDockerImageName())
                .cmd( // the container entrypoint sets the executable name
                        "-o",
                        "/recording/screen.flv",
                        "-P",
                        "/recording/password",
                        "vnchost",
                        String.valueOf(vncServiceContainer.getPort()))
                .hostConfig(HostConfig.builder()
                    // map the temporary directory to a volume in the container
                    .binds(tempDir.toAbsolutePath() + ":/recording:rw")
                    // link to the VNC-providing container with the hostname alias 'vnchost'
                    .links(vncServiceContainer.getContainerName() + ":vnchost")
                    .build())
                .build();
    }

    @Override
    protected String getDockerImageName() {
        return "richnorth/vnc-recorder:latest";
    }

    public Path getRecordingPath() {
        return this.tempDir.resolve("screen.flv");
    }

    /**
     * Stop the container and move the recording file to a suitable destination
     * @param destination the location the recording should be moved to, overwriting any existing file
     */
    public void stopAndRetainRecording(File destination) {
        super.stop();

        try {
            Files.move(getRecordingPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not move recording file from " + getRecordingPath() + " to " + destination, e);
        }
    }
}
