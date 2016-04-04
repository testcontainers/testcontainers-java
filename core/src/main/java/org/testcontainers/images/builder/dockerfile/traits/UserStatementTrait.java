package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;

public interface UserStatementTrait<SELF extends UserStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF user(String user) {
        return ((SELF) this).withStatement(new SingleArgumentStatement("USER", user));
    }
}
