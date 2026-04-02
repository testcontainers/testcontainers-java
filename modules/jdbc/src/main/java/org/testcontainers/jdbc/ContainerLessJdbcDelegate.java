package org.testcontainers.jdbc;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.exception.ConnectionCreationException;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Containerless jdbc database delegate
 *
 * Is used only with deprecated ScriptUtils
 *
 * @see org.testcontainers.ext.ScriptUtils
 */
@Slf4j
public class ContainerLessJdbcDelegate extends JdbcDatabaseDelegate {

    private Connection connection;

    public ContainerLessJdbcDelegate(Connection connection) {
        super(null, "");
        this.connection = connection;
    }

    @Override
    protected Statement createNewConnection() {
        try {
            return connection.createStatement();
        } catch (SQLException e) {
            log.error("Could create JDBC statement");
            throw new ConnectionCreationException("Could create JDBC statement", e);
        }
    }

    @Override
    protected void closeConnectionQuietly(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (Exception e) {
                log.error("Could not close JDBC connection", e);
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                log.error("Could not close JDBC connection", e);
            }
        }
    }
}
