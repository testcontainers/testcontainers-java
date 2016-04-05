package org.testcontainers.images.builder.dockerfile.statement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;

public class MultiArgsStatement extends Statement {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected final String[] args;

    public MultiArgsStatement(String type, String... args) {
        super(type);
        this.args = args;
    }

    @Override
    public void appendArguments(StringBuilder dockerfileStringBuilder) {
        try {
            dockerfileStringBuilder.append(objectMapper.writeValueAsString(args));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Can't serialize arguments: " + Arrays.toString(args), e);
        }
    }
}
