package org.testcontainers.controller.intents;

import com.github.dockerjava.api.model.Image;

import java.util.List;

public interface ListImagesIntent {
    List<Image> perform();
}
