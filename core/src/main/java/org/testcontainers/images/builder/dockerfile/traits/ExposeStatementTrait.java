package org.testcontainers.images.builder.dockerfile.traits;

import org.testcontainers.images.builder.dockerfile.statement.SingleArgumentStatement;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ExposeStatementTrait<SELF extends ExposeStatementTrait<SELF> & DockerfileBuilderTrait<SELF>> {

    default SELF expose(Integer... ports) {
        return ((SELF) this).withStatement(new SingleArgumentStatement("EXPOSE", Stream.of(ports).map(Object::toString).collect(Collectors.joining(" "))));
    }
}
