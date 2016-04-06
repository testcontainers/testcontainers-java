package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;

public interface CopyStatementTrait<SELF extends CopyStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF copy(String source, String destination) {
        return ((SELF) this).withStatement(new MultiArgsStatement("COPY", source, destination));
    }
}
