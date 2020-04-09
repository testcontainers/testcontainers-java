package org.testcontainers.containers;

import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
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
}
