package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public interface Neo4jTestImages {
    DockerImageName NEO4J_TEST_IMAGE = DockerImageName.parse("neo4j:4.4.1");
}
