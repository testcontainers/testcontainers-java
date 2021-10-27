package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;
import org.testcontainers.utility.DockerImageName;

public interface FromStatementTrait<SELF extends FromStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF from(String dockerImageName) {
        DockerImageName.parse(dockerImageName).assertValid();

        return ((SELF) this).withStatement(new SingleArgumentStatement("FROM", dockerImageName));
    }
}
