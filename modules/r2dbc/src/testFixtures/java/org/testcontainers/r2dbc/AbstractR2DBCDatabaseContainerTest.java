package org.testcontainers.r2dbc;

import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractR2DBCDatabaseContainerTest<T extends GenericContainer<?>> {

    protected abstract ConnectionFactoryOptions getOptions(T container);

    protected abstract String createR2DBCUrl();

    protected String createTestQuery(int result) {
        return String.format("SELECT %d", result);
    }

    @Test
    public final void testGetOptions() {
        try (T container = createContainer()) {
            container.start();

            ConnectionFactory connectionFactory = ConnectionFactories.get(getOptions(container));
            runTestQuery(connectionFactory);
        }
    }

    @Test
    public final void testUrlSupport() {
        ConnectionFactory connectionFactory = ConnectionFactories.get(createR2DBCUrl());
        runTestQuery(connectionFactory);
    }

    @Test
    public final void testGetMetadata() {
        ConnectionFactory connectionFactory = ConnectionFactories.get(createR2DBCUrl());
        ConnectionFactoryMetadata metadata = connectionFactory.getMetadata();
        assertThat(metadata).isNotNull();
    }

    protected abstract T createContainer();

    protected void runTestQuery(ConnectionFactory connectionFactory) {
        try {
            int expected = 42;
            Number result = Flux
                .usingWhen(
                    connectionFactory.create(),
                    connection -> connection.createStatement(createTestQuery(expected)).execute(),
                    Connection::close
                )
                .flatMap(it -> it.map((row, meta) -> (Number) row.get(0)))
                .blockFirst();

            assertThat(result)
                .isNotNull()
                .returns(expected, Number::intValue);
        } finally {
            if (connectionFactory instanceof Closeable) {
                Mono.from(((Closeable) connectionFactory).close()).block();
            }
        }
    }
}
