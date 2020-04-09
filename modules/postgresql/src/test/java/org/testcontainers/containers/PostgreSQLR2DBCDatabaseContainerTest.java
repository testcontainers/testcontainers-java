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

public class PostgreSQLR2DBCDatabaseContainerTest {

    @Test
    public void testGetOptions() {
        try (PostgreSQLContainer<?> container = new PostgreSQLContainer<>()) {
            container.start();

            int result = Flux
                .usingWhen(
                    Mono.just(
                        ConnectionFactories.get(
                            PostgreSQLR2DBCDatabaseContainer.getOptions(container)
                        )
                    ),
                    connectionFactory -> {
                        return Flux
                            .usingWhen(
                                connectionFactory.create(),
                                connection -> connection.createStatement("SELECT 42").execute(),
                                Connection::close
                            )
                            .flatMap(it -> it.map((row, meta) -> (Integer) row.get(0)));
                    },
                    it -> ((Closeable) it).close()
                )
                .blockFirst();

            assertEquals(42, result);
        }
    }

    @Test
    public void testUrlSupport() {
        String url = "r2dbc:tc:postgresql:///db?TC_IMAGE=postgres:10-alpine";
        ConnectionFactory connectionFactory = ConnectionFactories.get(url);
        try {
            int updated = Flux
                .usingWhen(
                    connectionFactory.create(),
                    connection -> {
                        return Mono
                            .from(connection.createStatement("CREATE TABLE test(id integer PRIMARY KEY)").execute())
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
