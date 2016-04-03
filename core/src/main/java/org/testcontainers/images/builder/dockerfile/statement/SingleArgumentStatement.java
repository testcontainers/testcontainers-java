package org.testcontainers.images.builder.dockerfile.statement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class SingleArgumentStatement extends Statement {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String argument;

    public SingleArgumentStatement(String type, String argument) {
        super(type);
        this.argument = argument;
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public void appendArguments(StringBuilder dockerfileStringBuilder) {
        String valueAsString = objectMapper.writeValueAsString(argument);
        dockerfileStringBuilder.append(valueAsString.substring(1, valueAsString.length() - 1));
    }
}
