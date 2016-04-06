package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;
import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;

public interface CmdStatementTrait<SELF extends CmdStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF cmd(String command) {
        return ((SELF) this).withStatement(new SingleArgumentStatement("CMD", command));
    }

    default SELF cmd(String... commandParts) {
        return ((SELF) this).withStatement(new MultiArgsStatement("CMD", commandParts));
    }
}
