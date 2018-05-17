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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.profiler.Profiler;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.builder.traits.*;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerLoggerFactory;
import org.testcontainers.utility.LazyFuture;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.Map;
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

        Profiler profiler = new Profiler("Rule creation - build image");
        profiler.setLogger(logger);

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

            profiler.start("Configure image");
            BuildImageCmd buildImageCmd = dockerClient.buildImageCmd(in);
            configure(buildImageCmd);

            profiler.start("Build image");
            BuildImageResultCallback exec = buildImageCmd.exec(resultCallback);

            // To build an image, we have to send the context to Docker in TAR archive format
            profiler.start("Send context as TAR");

            try (TarArchiveOutputStream tarArchive = new TarArchiveOutputStream(new GZIPOutputStream(out))) {
                tarArchive.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

                for (Map.Entry<String, Transferable> entry : transferables.entrySet()) {
                    Transferable transferable = entry.getValue();
                    final String destination = entry.getKey();
                    transferable.transferTo(tarArchive, destination);
                }
                tarArchive.finish();
            }

            profiler.start("Wait for an image id");
            exec.awaitImageId();

            return dockerImageName;
        } catch(IOException e) {
            throw new RuntimeException("Can't close DockerClient", e);
        } finally {
            profiler.stop().log();
        }
    }

    protected void configure(BuildImageCmd buildImageCmd) {
        buildImageCmd.withTag(this.getDockerImageName());
    }
}
