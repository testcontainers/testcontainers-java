package org.testcontainers.containers;

import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Frame;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.output.FrameConsumerResultCallback;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.Closeable;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

    private final Supplier<String> targetContainerIdSupplier;

    private String vncPassword = DEFAULT_VNC_PASSWORD;

    private int vncPort = 5900;

    private int frameRate = 30;

    public VncRecordingContainer(@NonNull GenericContainer<?> targetContainer) {
        this(targetContainer::getContainerId);
    }

    public VncRecordingContainer(@NonNull Supplier<String> targetContainerIdSupplier) {
        super(TestcontainersConfiguration.getInstance().getVncRecordedContainerImage());
        this.targetContainerIdSupplier = targetContainerIdSupplier;

        waitingFor(new AbstractWaitStrategy() {

            @Override
            protected void waitUntilReady() {
                try {
                    Unreliables.retryUntilTrue((int) startupTimeout.toMillis(), TimeUnit.MILLISECONDS, () -> {
                        CountDownLatch latch = new CountDownLatch(1);

                        FrameConsumerResultCallback callback = new FrameConsumerResultCallback() {
                            @Override
                            public void onNext(Frame frame) {
                                if (frame != null && new String(frame.getPayload()).contains("Connected")) {
                                    latch.countDown();
                                }
                            }
                        };

                        try (
                                Closeable __ = dockerClient.logContainerCmd(containerId)
                                        .withFollowStream(true)
                                        .withSince(0)
                                        .withStdErr(true)
                                        .exec(callback)
                        ) {
                            return latch.await(1, TimeUnit.SECONDS);
                        }
                    });
                } catch (TimeoutException e) {
                    throw new ContainerLaunchException("Timed out waiting for log output", e);
                }
            }
        });
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

        if (getNetwork() == null) {
            withNetwork(Network.SHARED);
        }

        String alias = "vnchost-" + Base58.randomString(8);
        dockerClient.connectToNetworkCmd()
                .withContainerId(targetContainerIdSupplier.get())
                .withNetworkId(getNetwork().getId())
                .withContainerNetwork(new ContainerNetwork().withAliases(alias))
                .exec();

        setCommand(
                "-c",
                "echo '" + Base64.encodeBase64String(vncPassword.getBytes()) + "' | base64 -d > /vnc_password && " +
                "flvrec.py -o " + RECORDING_FILE_NAME + " -d -r " + frameRate + " -P /vnc_password " + alias + " " + vncPort
        );
    }

    @Override
    public void stop() {
        try {
            dockerClient.disconnectFromNetworkCmd()
                    .withContainerId(targetContainerIdSupplier.get())
                    .withNetworkId(getNetwork().getId())
                    .withForce(true)
                    .exec();
        } catch (Exception e) {
            logger().warn("Failed to disconnect container with id '{}' from network '{}'", targetContainerIdSupplier.get(), getNetwork().getId(), e);
        }

        super.stop();
    }

    @SneakyThrows
    public InputStream streamRecording() {
        TarArchiveInputStream archiveInputStream = new TarArchiveInputStream(
                dockerClient.copyArchiveFromContainerCmd(containerId, RECORDING_FILE_NAME).exec()
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