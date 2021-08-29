package org.testcontainers.controller.model;

public class EnvironmentVariable {

    private final String name;
    private final String value;

    public EnvironmentVariable(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }
}
