package org.testcontainers;

import org.testcontainers.utility.DockerImageName;

public interface QuestDBTestImages {
    DockerImageName QUESTDB_IMAGE = DockerImageName.parse("questdb/questdb:6.5.3");
}
