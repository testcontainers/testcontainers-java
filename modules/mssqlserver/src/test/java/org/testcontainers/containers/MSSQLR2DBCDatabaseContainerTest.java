package org.testcontainers.containers;

import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.junit.Assert.*;

public class MSSQLR2DBCDatabaseContainerTest {

    @Test
    public void testGetOptions() {
        try (MSSQLServerContainer<?> container = new MSSQLServerContainer<>()) {
            container.start();

            ConnectionFactory connectionFactory = ConnectionFactories.get(
                MSSQLR2DBCDatabaseContainer.getOptions(container)
            );

            int result = Flux
                .usingWhen(
                    connectionFactory.create(),
                    connection -> connection.createStatement("SELECT 42").execute(),
                    Connection::close
                )
                .flatMap(it -> it.map((row, meta) -> (Integer) row.get(0)))
                .blockFirst();

            assertEquals(42, result);
        }
    }

    @Test
    public void testUrlSupport() {
        String url = "r2dbc:tc:sqlserver:///?TC_IMAGE=mcr.microsoft.com%2Fmssql%2Fserver%3A2017-CU12";
        ConnectionFactory connectionFactory = ConnectionFactories.get(url);
        try {
            int updated = Flux
                .usingWhen(
                    connectionFactory.create(),
                    connection -> {
                        return Mono
                            .from(connection.createStatement("CREATE DATABASE [test];").execute())
                            .thenMany(connection.createStatement("CREATE TABLE test(id integer PRIMARY KEY)").execute())
                            .thenMany(connection.createStatement("INSERT INTO test(id) VALUES(123)").execute())
                            .flatMap(Result::getRowsUpdated);
                    },
                    Connection::close
                )
                .blockFirst();

            assertEquals(updated, 1);
        } finally {
            Mono.from(((Closeable) connectionFactory).close()).block();
        }
    }
}
