package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.Statement;

import java.util.List;

public interface DockerfileBuilderTrait<SELF extends DockerfileBuilderTrait<SELF>> {

    List<Statement> getStatements();

    default SELF withStatement(Statement statement) {
        getStatements().add(statement);

        return (SELF) this;
    }
}
