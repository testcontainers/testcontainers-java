package org.testcontainers.docker.intents;

import com.github.dockerjava.api.command.ListImagesCmd;
import com.github.dockerjava.api.model.Image;
import org.testcontainers.controller.intents.ListImagesIntent;

import java.util.List;

public class ListImagesDockerIntent implements ListImagesIntent {
    private final ListImagesCmd listImagesCmd;

    public ListImagesDockerIntent(ListImagesCmd listImagesCmd) {
        this.listImagesCmd = listImagesCmd;
    }

    @Override
    public List<Image> perform() {
        return listImagesCmd.exec();
    }
}
