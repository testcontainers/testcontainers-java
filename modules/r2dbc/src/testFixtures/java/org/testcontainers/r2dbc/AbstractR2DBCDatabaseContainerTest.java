package org.testcontainers.r2dbc;

import com.github.dockerjava.api.DockerClient;
import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.r2dbc.Hidden.TestcontainersR2DBCConnectionFactoryProvider;
import org.testcontainers.utility.TestcontainersConfigurationRollbackRule;
import org.testcontainers.utility.TestcontainersConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractR2DBCDatabaseContainerTest<T extends GenericContainer<?>> {

    @Rule
    public final TestcontainersConfigurationRollbackRule configurationMock = new TestcontainersConfigurationRollbackRule();

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
    public void supportsAliases() throws Exception {
        String alias = UUID.randomUUID().toString();
        String testImage = "local/testcontainers/db_image";
        String tag = UUID.randomUUID().toString();

        Properties properties = new Properties(TestcontainersConfiguration.getInstance().getProperties());
        properties.put("db.alias." + alias + ".image", testImage);
        properties.put(
            "db.alias." + alias + ".r2dbcDriver",
            TestcontainersR2DBCConnectionFactoryProvider
                .removeProxying(ConnectionFactoryOptions.parse(createR2DBCUrl()))
                .getRequiredValue(ConnectionFactoryOptions.DRIVER)
        );

        Mockito.doReturn(properties).when(TestcontainersConfiguration.getInstance()).getProperties();

        String imageId = createContainer().getDockerImageName();
        DockerClient client = DockerClientFactory.instance().client();
        client.tagImageCmd(imageId, testImage, tag).exec();

        String image = testImage + ":" + tag;
        imageTagged(image);

        try (
            AutoCloseable cleanup = () -> {
                client.removeImageCmd(image).withForce(true).exec();
            }
        ) {
            ConnectionFactory connectionFactory = ConnectionFactories.get(
                ConnectionFactoryOptions.parse(createR2DBCUrl())
                    .mutate()
                    .option(ConnectionFactoryOptions.PROTOCOL, alias)
                    .option(R2DBCDatabaseContainerProvider.IMAGE_TAG_OPTION, tag)
                    .build()
            );
            runTestQuery(connectionFactory);
        }
    }

    protected void imageTagged(String image) {
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
