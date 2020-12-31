package org.testcontainers.vault;

/**
 * Vault preset of logging levels.
 */
public enum VaultLogLevel {
    Trace("trace"), Debug("debug"), Info("info"), Warn("warn"), Error("err");

    public final String config;

    VaultLogLevel(String config) {
        this.config = config;
    }
}
