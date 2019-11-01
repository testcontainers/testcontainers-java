package org.testcontainers.containers.image;

import java.time.Instant;
import java.util.List;
import java.util.OptionalLong;

public interface ImageData {

    Instant getCreated();

    String getId();

    String getParentId();

    List<String> getRepoTags();

    List<String> getRepoDigests();

    OptionalLong getSize();

    OptionalLong getVirtualSize();


}
