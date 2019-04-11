package org.testcontainers.containers.image;

import java.util.List;

public interface ImageData {

    Long getCreated();

    String getId();

    String getParentId();

    List<String> getRepoTags();

    List<String> getRepoDigests();

    Long getSize();

    Long getVirtualSize();


}
