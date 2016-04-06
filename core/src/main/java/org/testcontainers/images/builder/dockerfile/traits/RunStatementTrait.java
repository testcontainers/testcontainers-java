package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;
import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;

public interface RunStatementTrait<SELF extends RunStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF run(String... commandParts) {
        return ((SELF) this).withStatement(new MultiArgsStatement("RUN", commandParts));
    }

    default SELF run(String command) {
        return ((SELF) this).withStatement(new SingleArgumentStatement("RUN", command));
    }
}
