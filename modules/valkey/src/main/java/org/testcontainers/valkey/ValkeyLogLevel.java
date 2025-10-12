package org.testcontainers.valkey;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ValkeyLogLevel {
    DEBUG("debug"),
    VERBOSE("verbose"),
    NOTICE("notice"),
    WARNING("warning");

    private final String level;
}
