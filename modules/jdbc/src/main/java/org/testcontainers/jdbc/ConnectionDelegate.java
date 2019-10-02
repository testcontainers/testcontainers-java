package org.testcontainers.jdbc;

import lombok.Data;
import lombok.experimental.Delegate;

import java.sql.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

class ConnectionDelegate implements Connection {
    @Delegate(types=Connection.class)
    private final Connection delegate;

    public ConnectionDelegate(Connection connection){
        this.delegate = connection;
    }
}
