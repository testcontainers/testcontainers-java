package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;

public interface WorkdirStatementTrait<SELF extends WorkdirStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF workDir(String workdir) {
        return ((SELF) this).withStatement(new SingleArgumentStatement("WORKDIR", workdir));
    }
}
