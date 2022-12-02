package org.testcontainers.dockerclient;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateNetworkCmd;
import com.github.dockerjava.api.command.KillContainerCmd;
import com.github.dockerjava.api.command.RemoveContainerCmd;
import com.github.dockerjava.api.command.RemoveNetworkCmd;
import com.github.dockerjava.api.command.StartContainerCmd;
import com.github.dockerjava.api.command.StopContainerCmd;
import com.github.dockerjava.api.command.SyncDockerCmd;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.AuditLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.function.BiConsumer;

/**
 * Wrapper for {@link DockerClient} to facilitate 'audit logging' of potentially destruction actions using
 * {@link org.testcontainers.utility.AuditLogger}.
 *
 */
@Slf4j
@SuppressWarnings("unchecked")
class AuditLoggingDockerClient implements DockerClient {

    @Delegate(excludes = InterceptedMethods.class)
    private final DockerClient wrappedClient;

    public AuditLoggingDockerClient(DockerClient wrappedClient) {
        this.wrappedClient = wrappedClient;
    }

    @Override
    public CreateContainerCmd createContainerCmd(@NotNull String image) {
        return wrappedCommand(
            CreateContainerCmd.class,
            wrappedClient.createContainerCmd(image),
            (cmd, res) -> AuditLogger.doLog("CREATE", image, res.getId(), cmd),
            (cmd, e) -> AuditLogger.doLog("CREATE", image, null, cmd, e)
        );
    }

    @Override
    public StartContainerCmd startContainerCmd(@NotNull String containerId) {
        return wrappedCommand(
            StartContainerCmd.class,
            wrappedClient.startContainerCmd(containerId),
            (cmd, res) -> AuditLogger.doLog("START", null, containerId, cmd),
            (cmd, e) -> AuditLogger.doLog("START", null, containerId, cmd, e)
        );
    }

    @Override
    public RemoveContainerCmd removeContainerCmd(@NotNull String containerId) {
        return wrappedCommand(
            RemoveContainerCmd.class,
            wrappedClient.removeContainerCmd(containerId),
            (cmd, res) -> AuditLogger.doLog("REMOVE", null, containerId, cmd),
            (cmd, e) -> AuditLogger.doLog("REMOVE", null, containerId, cmd, e)
        );
    }

    @Override
    public StopContainerCmd stopContainerCmd(@NotNull String containerId) {
        return wrappedCommand(
            StopContainerCmd.class,
            wrappedClient.stopContainerCmd(containerId),
            (cmd, res) -> AuditLogger.doLog("STOP", null, containerId, cmd),
            (cmd, e) -> AuditLogger.doLog("STOP", null, containerId, cmd, e)
        );
    }

    @Override
    public KillContainerCmd killContainerCmd(@NotNull String containerId) {
        return wrappedCommand(
            KillContainerCmd.class,
            wrappedClient.killContainerCmd(containerId),
            (cmd, res) -> AuditLogger.doLog("KILL", null, containerId, cmd),
            (cmd, e) -> AuditLogger.doLog("KILL", null, containerId, cmd, e)
        );
    }

    @Override
    public CreateNetworkCmd createNetworkCmd() {
        return wrappedCommand(
            CreateNetworkCmd.class,
            wrappedClient.createNetworkCmd(),
            (cmd, res) -> AuditLogger.doLog("CREATE_NETWORK", null, null, cmd),
            (cmd, e) -> AuditLogger.doLog("CREATE_NETWORK", null, null, cmd, e)
        );
    }

    @Override
    public RemoveNetworkCmd removeNetworkCmd(@NotNull String networkId) {
        return wrappedCommand(
            RemoveNetworkCmd.class,
            wrappedClient.removeNetworkCmd(networkId),
            (cmd, res) -> AuditLogger.doLog("REMOVE_NETWORK", null, null, cmd),
            (cmd, e) -> AuditLogger.doLog("REMOVE_NETWORK", null, null, cmd, e)
        );
    }

    private <T extends SyncDockerCmd<R>, R> T wrappedCommand(
        Class<T> clazz,
        T cmd,
        BiConsumer<T, R> successConsumer,
        BiConsumer<T, Exception> failureConsumer
    ) {
        return (T) Proxy.newProxyInstance(
            clazz.getClassLoader(),
            new Class<?>[] { clazz },
            (proxy, method, args) -> {
                if (method.getName().equals("exec")) {
                    try {
                        R r = (R) method.invoke(cmd, args);
                        successConsumer.accept(cmd, r);
                        return r;
                    } catch (Exception e) {
                        if (e instanceof InvocationTargetException && e.getCause() instanceof Exception) {
                            e = (Exception) e.getCause();
                        }
                        failureConsumer.accept(cmd, e);
                        throw e;
                    }
                } else {
                    return method.invoke(cmd, args);
                }
            }
        );
    }

    @SuppressWarnings("unused")
    private interface InterceptedMethods {
        CreateContainerCmd createContainerCmd(String image);

        StartContainerCmd startContainerCmd(String containerId);

        RemoveContainerCmd removeContainerCmd(String containerId);

        StopContainerCmd stopContainerCmd(String containerId);

        KillContainerCmd killContainerCmd(String containerId);

        CreateNetworkCmd createNetworkCmd();

        RemoveNetworkCmd removeNetworkCmd(String networkId);
    }
}
