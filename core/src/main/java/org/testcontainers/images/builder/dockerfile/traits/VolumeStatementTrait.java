package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;

public interface VolumeStatementTrait<SELF extends VolumeStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF volume(String... volumes) {
        return ((SELF) this).withStatement(new MultiArgsStatement("VOLUME", volumes));
    }
}
