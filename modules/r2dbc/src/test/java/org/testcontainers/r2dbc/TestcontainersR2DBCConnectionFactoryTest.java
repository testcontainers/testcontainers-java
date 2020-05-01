package org.testcontainers.r2dbc;

import io.r2dbc.postgresql.api.PostgresqlException;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class TestcontainersR2DBCConnectionFactoryTest {

    @Test
    public void failsOnUnknownProvider() {
        String nonExistingProvider = UUID.randomUUID().toString();
        Assertions
            .assertThatThrownBy(() -> {
                ConnectionFactories.get(String.format("r2dbc:tc:%s:///db", nonExistingProvider));
            })
            .hasMessageContaining("Missing provider")
            .hasMessageContaining(nonExistingProvider);
    }

    @Test
    public void reusesUntilConnectionFactoryIsClosed() {
        String url = "r2dbc:tc:postgresql:///db?TC_IMAGE_TAG=10-alpine";
        ConnectionFactory connectionFactory = ConnectionFactories.get(url);

        Integer updated = Flux
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

        assertThat(updated).isEqualTo(1);

        Flux<Long> select = Flux
            .usingWhen(
                Flux.defer(connectionFactory::create),
                connection -> {
                    return Flux
                        .from(connection.createStatement("SELECT COUNT(*) FROM test").execute())
                        .flatMap(it -> it.map((row, meta) -> (Long) row.get(0)));
                },
                Connection::close
            );

        Long rows = select.blockFirst();

        assertThat(rows).isEqualTo(1);

        close(connectionFactory);

        Assertions
            .assertThatThrownBy(select::blockFirst)
            .isInstanceOf(PostgresqlException.class)
            // relation "X" does not exists
            // https://github.com/postgres/postgres/blob/REL_10_0/src/backend/utils/errcodes.txt#L349
            .returns("42P01", e -> ((PostgresqlException) e).getErrorDetails().getCode());
    }

    private static void close(ConnectionFactory connectionFactory) {
        Mono.from(((Closeable) connectionFactory).close()).block();
    }
}
