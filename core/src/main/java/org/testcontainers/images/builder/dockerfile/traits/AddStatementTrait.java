package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;

public interface AddStatementTrait<SELF extends AddStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF add(String source, String destination) {
        return ((SELF) this).withStatement(new MultiArgsStatement("ADD", source, destination));
    }
}
