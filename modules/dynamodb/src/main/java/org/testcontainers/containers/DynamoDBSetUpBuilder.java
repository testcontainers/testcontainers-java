package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
class DynamoDBSetUpBuilder {

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Helper {

        @NonNull
        private Supplier<DynamoDbClientBuilder> builderSupplier;

        private final List<Function<DynamoDbClientBuilder, DynamoDbClientBuilder>> builders = new ArrayList<>();

        private final List<BiConsumer<DynamoDbClient, InspectContainerResponse>> clients = new ArrayList<>();

        public Helper withClient(final Function<DynamoDbClientBuilder, DynamoDbClientBuilder> builderSetUp) {
            this.builders.add(builderSetUp);
            return this;
        }

        public Helper withClientRegion(final Region region) {
            return withClient(b -> b.region(region));
        }

        public Helper withClientCredentials(final String accessKeyId, final String secretKeyId) {
            return withClient(b ->
                b.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKeyId))
                )
            );
        }

        public Helper withSetUp(final BiConsumer<DynamoDbClient, InspectContainerResponse> clientSetUp) {
            this.clients.add(clientSetUp);
            return this;
        }

        public Helper withSetUp(final Consumer<DynamoDbClient> clientSetUp) {
            return withSetUp((dynamoDbClient, inspectContainerResponse) -> clientSetUp.accept(dynamoDbClient));
        }

        protected void run(InspectContainerResponse containerInfo) {
            var builder = builderSupplier.get();

            for (Function<DynamoDbClientBuilder, DynamoDbClientBuilder> builderFunction : builders) {
                builder = builderFunction.apply(builder);
            }

            try (val client = builder.build()) {
                for (BiConsumer<DynamoDbClient, InspectContainerResponse> consumer : clients) {
                    consumer.accept(client, containerInfo);
                }
            }
        }
    }

    private final List<Helper> helpers = new ArrayList<>();

    public void run(InspectContainerResponse containerInfo) {
        for (Helper helper : helpers) {
            helper.run(containerInfo);
        }
    }

    public void addSetUp(final Supplier<DynamoDbClientBuilder> builder, final Consumer<Helper> helper) {
        val helperInstance = new Helper(builder);

        helper.accept(helperInstance);

        this.helpers.add(helperInstance);
    }
}
