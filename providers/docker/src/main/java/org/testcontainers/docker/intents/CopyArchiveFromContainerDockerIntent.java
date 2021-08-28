package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.CopyArchiveFromContainerCmd;
import org.testcontainers.controller.intents.CopyArchiveFromContainerIntent;

import java.io.InputStream;

public class CopyArchiveFromContainerDockerIntent implements CopyArchiveFromContainerIntent {
    private final CopyArchiveFromContainerCmd copyArchiveFromContainerCmd;

    public CopyArchiveFromContainerDockerIntent(CopyArchiveFromContainerCmd copyArchiveFromContainerCmd) {
        this.copyArchiveFromContainerCmd = copyArchiveFromContainerCmd;
    }

    @Override
    public InputStream perform() {
        return copyArchiveFromContainerCmd.exec();
    }
}
