package org.testcontainers.containers;

import lombok.Getter;

@Getter
public enum AzuriteService {
    BLOB(10000),
    QUEUE(10001),
    TABLE(10002);

    private final int port;

    AzuriteService(int port) {
        this.port = port;
    }
}
