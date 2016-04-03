package org.testcontainers.images.builder.dockerfile.statement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class MultiArgsStatement extends Statement {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String[] args;

    public MultiArgsStatement(String type, String... args) {
        super(type);
        this.args = args;
    }

    @Override
    @SneakyThrows(JsonProcessingException.class)
    public void appendArguments(StringBuilder dockerfileStringBuilder) {
        dockerfileStringBuilder.append(objectMapper.writeValueAsString(args));
    }
}
