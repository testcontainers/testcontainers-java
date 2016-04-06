package org.testcontainers.images.builder.dockerfile.statement;

public class RawStatement extends Statement {

    final String rawValue;

    public RawStatement(String type, String rawValue) {
        super(type);
        this.rawValue = rawValue;
    }

    @Override
    public void appendArguments(StringBuilder dockerfileStringBuilder) {
        dockerfileStringBuilder.append(rawValue);
    }
}
