package org.testcontainers.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContainerLessJdbcDelegateTest {

    @Test
    void closeClosesStatementAndConnectionQuietly() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        ContainerLessJdbcDelegate delegate = new ContainerLessJdbcDelegate(connection);
        delegate.execute("select 1", "test.sql", 1, false, false);

        assertThatCode(delegate::close).doesNotThrowAnyException();

        verify(statement).close();
        verify(connection).close();
    }
}