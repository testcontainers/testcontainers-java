package org.testcontainers.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionWrapper extends ConnectionDelegate {

    private final Runnable closeCallback;

    public ConnectionWrapper(Connection connection, Runnable runnable) {
        super(connection);
        this.closeCallback = runnable;
    }

    @Override
    public void close() throws SQLException {
        super.close();
        try {
            closeCallback.run();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
