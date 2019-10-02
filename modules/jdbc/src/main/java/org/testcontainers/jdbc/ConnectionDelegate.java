package org.testcontainers.jdbc;

import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.experimental.Delegate;

import java.sql.Connection;

@AllArgsConstructor
class ConnectionDelegate implements Connection {
    @Delegate
    Connection delegate;
}
