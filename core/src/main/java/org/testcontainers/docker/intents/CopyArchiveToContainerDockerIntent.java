package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.CopyArchiveToContainerCmd;
import org.testcontainers.controller.intents.CopyArchiveToContainerIntent;

import java.io.InputStream;

public class CopyArchiveToContainerDockerIntent implements CopyArchiveToContainerIntent {
    private final CopyArchiveToContainerCmd copyArchiveToContainerCmd;

    public CopyArchiveToContainerDockerIntent(CopyArchiveToContainerCmd copyArchiveToContainerCmd) {
        this.copyArchiveToContainerCmd = copyArchiveToContainerCmd;
    }

    @Override
    public CopyArchiveToContainerIntent withTarInputStream(InputStream tarInputStream) {
        copyArchiveToContainerCmd.withTarInputStream(tarInputStream);
        return this;
    }

    @Override
    public CopyArchiveToContainerIntent withRemotePath(String remotePath) {
        copyArchiveToContainerCmd.withRemotePath(remotePath);
        return this;
    }

    @Override
    public void perform() {
        copyArchiveToContainerCmd.exec();
    }
}
