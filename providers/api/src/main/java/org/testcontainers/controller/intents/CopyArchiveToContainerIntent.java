package org.testcontainers.controller.intents;

import java.io.InputStream;

public interface CopyArchiveToContainerIntent {
    CopyArchiveToContainerIntent withTarInputStream(InputStream tarInputStream);

    CopyArchiveToContainerIntent withRemotePath(String remotePath);

    void perform();
}
