package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.MultiArgsStatement;
import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;

public interface EntryPointStatementTrait<SELF extends EntryPointStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF entryPoint(String command) {
        return ((SELF) this).withStatement(new SingleArgumentStatement("ENTRYPOINT", command));
    }

    default SELF entryPoint(String... commandParts) {
        return ((SELF) this).withStatement(new MultiArgsStatement("ENTRYPOINT", commandParts));
    }
}
