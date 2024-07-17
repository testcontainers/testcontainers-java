package org.testcontainers.containers;

import com.google.common.base.Joiner;
import com.google.common.util.concurrent.Uninterruptibles;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.startupcheck.IndefiniteWaitOneShotStartupCheckStrategy;
import org.testcontainers.utility.AuditLogger;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import org.testcontainers.utility.PathUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Use Docker Compose container.
 */
class ContainerisedDockerCompose extends GenericContainer<ContainerisedDockerCompose> implements DockerCompose {

    public static final char UNIX_PATH_SEPARATOR = ':';

    public ContainerisedDockerCompose(
        DockerImageName dockerImageName,
        List<File> composeFiles,
        String identifier,
        List<String> fileCopyInclusions
    ) {
        super(dockerImageName);
        addEnv(ENV_PROJECT_NAME, identifier);

        // Map the docker compose file into the container
        final File dockerComposeBaseFile = composeFiles.get(0);
        final String pwd = dockerComposeBaseFile.getAbsoluteFile().getParentFile().getAbsolutePath();
        final String containerPwd = convertToUnixFilesystemPath(pwd);

        final List<String> absoluteDockerComposeFiles = composeFiles
            .stream()
            .map(File::getAbsolutePath)
            .map(MountableFile::forHostPath)
            .map(MountableFile::getFilesystemPath)
            .map(this::convertToUnixFilesystemPath)
            .collect(Collectors.toList());
        final String composeFileEnvVariableValue = Joiner.on(UNIX_PATH_SEPARATOR).join(absoluteDockerComposeFiles); // we always need the UNIX path separator
        logger().debug("Set env COMPOSE_FILE={}", composeFileEnvVariableValue);
        addEnv(ENV_COMPOSE_FILE, composeFileEnvVariableValue);
        if (fileCopyInclusions.isEmpty()) {
            logger().info("Copying all files in {} into the container", pwd);
            withCopyFileToContainer(MountableFile.forHostPath(pwd), containerPwd);
        } else {
            // Always copy the compose file itself
            logger().info("Copying docker compose file: {}", dockerComposeBaseFile.getAbsolutePath());
            withCopyFileToContainer(
                MountableFile.forHostPath(dockerComposeBaseFile.getAbsolutePath()),
                convertToUnixFilesystemPath(dockerComposeBaseFile.getAbsolutePath())
            );
            for (String pathToCopy : fileCopyInclusions) {
                String hostPath = pwd + "/" + pathToCopy;
                logger().info("Copying inclusion file: {}", hostPath);
                withCopyFileToContainer(MountableFile.forHostPath(hostPath), convertToUnixFilesystemPath(hostPath));
            }
        }

        // Ensure that compose can access docker. Since the container is assumed to be running on the same machine
        //  as the docker daemon, just mapping the docker control socket is OK.
        // As there seems to be a problem with mapping to the /var/run directory in certain environments (e.g. CircleCI)
        //  we map the socket file outside of /var/run, as just /docker.sock
        addFileSystemBind(
            DockerClientFactory.instance().getRemoteDockerUnixSocketPath(),
            "/docker.sock",
            BindMode.READ_WRITE
        );
        addEnv("DOCKER_HOST", "unix:///docker.sock");
        setStartupCheckStrategy(new IndefiniteWaitOneShotStartupCheckStrategy());
        setWorkingDirectory(containerPwd);
    }

    @Override
    public void invoke() {
        super.start();

        this.followOutput(new Slf4jLogConsumer(logger()));

        // wait for the compose container to stop, which should only happen after it has spawned all the service containers
        logger()
            .info("Docker Compose container is running for command: {}", Joiner.on(" ").join(this.getCommandParts()));
        while (this.isRunning()) {
            logger().trace("Compose container is still running");
            Uninterruptibles.sleepUninterruptibly(100, TimeUnit.MILLISECONDS);
        }
        logger().info("Docker Compose has finished running");

        AuditLogger.doComposeLog(this.getCommandParts(), this.getEnv());

        final Integer exitCode = getDockerClient()
            .inspectContainerCmd(getContainerId())
            .exec()
            .getState()
            .getExitCode();

        if (exitCode == null || exitCode != 0) {
            throw new ContainerLaunchException(
                "Containerised Docker Compose exited abnormally with code " +
                exitCode +
                " whilst running command: " +
                StringUtils.join(this.getCommandParts(), ' ')
            );
        }
    }

    private String convertToUnixFilesystemPath(String path) {
        return SystemUtils.IS_OS_WINDOWS ? PathUtils.createMinGWPath(path).substring(1) : path;
    }
}
