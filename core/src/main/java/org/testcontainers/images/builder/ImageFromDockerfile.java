package org.testcontainers.images.builder;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.BuildImageCmd;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.model.BuildResponseItem;
import com.github.dockerjava.core.command.BuildImageResultCallback;
import com.google.common.collect.Sets;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.builder.traits.*;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

@Slf4j
@Getter
public class ImageFromDockerfile extends LazyFuture<String> implements
        BuildContextBuilderTrait<ImageFromDockerfile>,
        ClasspathTrait<ImageFromDockerfile>,
        FilesTrait<ImageFromDockerfile>,
        StringsTrait<ImageFromDockerfile>,
        DockerfileTrait<ImageFromDockerfile> {

    private static final Set<String> imagesToDelete = Sets.newConcurrentHashSet();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(DockerClientFactory.TESTCONTAINERS_THREAD_GROUP, () -> {
            DockerClient dockerClientForCleaning = DockerClientFactory.instance().client();
            try {
                for (String dockerImageName : imagesToDelete) {
                    log.info("Removing image tagged {}", dockerImageName);
                    try {
                        dockerClientForCleaning.removeImageCmd(dockerImageName).withForce(true).exec();
                    } catch (Throwable e) {
                        log.warn("Unable to delete image " + dockerImageName, e);
                    }
                }
            } catch (DockerClientException e) {
                throw new RuntimeException(e);
            }
        }));
    }

    private final String dockerImageName;

    private boolean deleteOnExit = true;

    private final Map<String, Transferable> transferables = new HashMap<>();
    private final Map<String, String> buildArgs = new HashMap<>();
    private Optional<String> dockerFilePath = Optional.empty();
    private Optional<Path> dockerfile = Optional.empty();

    public ImageFromDockerfile() {
        this("testcontainers/" + Base58.randomString(16).toLowerCase());
    }

    public ImageFromDockerfile(String dockerImageName) {
        this(dockerImageName, true);
    }

    public ImageFromDockerfile(String dockerImageName, boolean deleteOnExit) {
        this.dockerImageName = dockerImageName;
        this.deleteOnExit = deleteOnExit;
    }

    @Override
    public ImageFromDockerfile withFileFromTransferable(String path, Transferable transferable) {
        Transferable oldValue = transferables.put(path, transferable);

        if (oldValue != null) {
            log.warn("overriding previous mapping for '{}'", path);
        }

        return this;
    }

    @Override
    protected final String resolve() {
        Logger logger = DockerLoggerFactory.getLogger(dockerImageName);

        DockerClient dockerClient = DockerClientFactory.instance().client();
        try {
            if (deleteOnExit) {
                imagesToDelete.add(dockerImageName);
            }

            BuildImageResultCallback resultCallback = new BuildImageResultCallback() {
                @Override
                public void onNext(BuildResponseItem item) {
                    super.onNext(item);

                    if (item.isErrorIndicated()) {
                        logger.error(item.getErrorDetail().getMessage());
                    } else {
                        logger.debug(StringUtils.chomp(item.getStream(), "\n"));
                    }
                }
            };

            // We have to use pipes to avoid high memory consumption since users might want to build really big images
            @Cleanup PipedInputStream in = new PipedInputStream();
            @Cleanup PipedOutputStream out = new PipedOutputStream(in);

            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(in);
            configure(buildImageCmd);

            BuildImageResultCallback exec = buildImageCmd.exec(resultCallback);
            
            long bytesToDockerDaemon = 0;

            // To build an image, we have to send the context to Docker in TAR archive format
            try (TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(new GZIPOutputStream(out))) {
                tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                for (Map.Entry<String, Transferable> entry : transferables.entrySet()) {
                    Transferable transferable = entry.getValue();
                    final String destination = entry.getKey();
                    transferable.transferTo(tarArchive, destination);
                    bytesToDockerDaemon += transferable.getSize();
                }
                tarArchive.finish();
            }
            
            log.info("Transferred {} KB to Docker daemon", FileUtils.byteCountToDisplaySize(bytesToDockerDaemon));
            if (bytesToDockerDaemon > FileUtils.ONE_MB * 50) // warn if >50MB sent to docker daemon
                log.warn("A large amount of data was sent to the Docker daemon ({}). Consider using a .dockerignore file for better performance.", 
                        FileUtils.byteCountToDisplaySize(bytesToDockerDaemon));

            exec.awaitImageId();

            return dockerImageName;
        } catch(IOException e) {
            throw new RuntimeException("Can't close DockerClient", e);
        }
    }

    protected void configure(BuildImageCmd buildImageCmd) {
        buildImageCmd.withTag(this.getDockerImageName());
        this.dockerFilePath.ifPresent(buildImageCmd::withDockerfilePath);
        this.dockerfile.ifPresent(p -> buildImageCmd.withDockerfile(p.toFile()));
        this.buildArgs.forEach(buildImageCmd::withBuildArg);
    }

    public ImageFromDockerfile withBuildArg(final String key, final String value) {
        this.buildArgs.put(key, value);
        return this;
    }

    public ImageFromDockerfile withBuildArgs(final Map<String, String> args) {
        this.buildArgs.putAll(args);
        return this;
    }

    /**
     * Sets the Dockerfile to be used for this image. 
     * @deprecated It is recommended to use {@link #withDockerfile} instead because it honors 
     * .dockerignore files and therefore will be more efficient
     * @param relativePathFromBuildRoot
     */
    @Deprecated
    public ImageFromDockerfile withDockerfilePath(String relativePathFromBuildRoot) {
        this.dockerFilePath = Optional.of(relativePathFromBuildRoot);
        return this;
    }
    
    /**
     * Sets the Dockerfile to be used for this image. 
     * @param dockerfile
     */
    public ImageFromDockerfile withDockerfile(Path dockerfile) {
        this.dockerfile = Optional.of(dockerfile);
        return this;
    }
    
}
