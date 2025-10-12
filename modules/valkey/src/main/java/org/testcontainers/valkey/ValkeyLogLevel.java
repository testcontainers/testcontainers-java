package org.testcontainers.valkey;

public enum ValkeyLogLevel {
    DEBUG("debug"),
    VERBOSE("verbose"),
    NOTICE("notice"),
    WARNING("warning");

    private final String level;

    ValkeyLogLevel(String level) {
        this.level = level;
    }

    public String getLevel() {
        return level;
    }
}
