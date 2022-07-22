package org.testcontainers.junit.questdb;

import org.testcontainers.utility.DockerImageName;

public interface QuestDBTestImages {
    DockerImageName QUESTDB_TEST_IMAGE = DockerImageName.parse("questdb/questdb:6.4.3");
}
