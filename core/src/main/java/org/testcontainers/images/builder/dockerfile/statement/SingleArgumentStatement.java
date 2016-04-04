package org.testcontainers.images.builder.dockerfile.statement;

public class SingleArgumentStatement extends Statement {

    protected final String argument;

    public SingleArgumentStatement(String type, String argument) {
        super(type);
        this.argument = argument;
    }

    @Override
    public void appendArguments(StringBuilder dockerfileStringBuilder) {
        dockerfileStringBuilder.append(argument.replace("\n", "\\\n"));
    }
}
