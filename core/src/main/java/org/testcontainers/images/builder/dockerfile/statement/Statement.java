package org.testcontainers.images.builder.dockerfile.statement;

import lombok.Data;

@Data
public abstract class Statement {

    final String type;

    public abstract void appendArguments(StringBuilder dockerfileStringBuilder);
}
