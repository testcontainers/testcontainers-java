package org.testcontainers.containers.image;

import java.time.Instant;
import java.util.List;

public interface ImageData {

    Instant getCreated();

    String getId();

    String getParentId();

    List<String> getRepoTags();

    List<String> getRepoDigests();

    Long getSize();

    Long getVirtualSize();


}
