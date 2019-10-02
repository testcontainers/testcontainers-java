package org.testcontainers.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Delegate;

import java.sql.Connection;

@RequiredArgsConstructor
class ConnectionDelegate implements Connection {
    @Delegate
    private final Connection delegate;
}
