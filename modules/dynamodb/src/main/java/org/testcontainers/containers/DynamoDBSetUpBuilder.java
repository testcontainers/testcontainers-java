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
import java.util.function.Supplier;

@Slf4j
class DynamoDBSetUpBuilder {

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Helper {

        @NonNull
        private Supplier<DynamoDbClientBuilder> builderSupplier;

        private final List<Consumer<DynamoDbClientBuilder>> builders = new ArrayList<>();

        private final List<BiConsumer<DynamoDbClient, InspectContainerResponse>> clients = new ArrayList<>();

        /**
         * Configure the builder for the next {@link #withSetUp(Consumer)} client operations.
         *
         * @param builderSetUp functional method to configure the current builder.
         * @return self instance.
         */
        public Helper withBuilder(final Consumer<DynamoDbClientBuilder> builderSetUp) {
            this.builders.add(builderSetUp);
            return this;
        }

        /**
         * Set the current region for this Helper instance.
         * @param region Set the current builder region for this instance
         * @return self instance.
         */
        public Helper withBuilderRegion(final Region region) {
            return withBuilder(b -> b.region(region));
        }

        /**
         * Set the current credentials for this Helper instance.
         * @param accessKeyId The AWS access-key for the client
         * @param secretKeyId The AWS secret-key for the cleint
         * @return self instance.
         */
        public Helper withBuilderCredentials(final String accessKeyId, final String secretKeyId) {
            return withBuilder(b -> {
                b.credentialsProvider(
                    StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretKeyId))
                );
            });
        }

        /**
         * Functional method to operate with current DynamoDB container when the container is running.
         *
         * @param clientSetUp BiConsumer with the {@link DynamoDbClient} and {@link InspectContainerResponse}
         * @return self instance.
         */
        public Helper withSetUp(final BiConsumer<DynamoDbClient, InspectContainerResponse> clientSetUp) {
            this.clients.add(clientSetUp);
            return this;
        }

        /**
         * Functional method to operate with current DynamoDB container when the container is running.
         *
         * @param clientSetUp Consumer with the {@link DynamoDbClient}
         * @return self instance.
         */
        public Helper withSetUp(final Consumer<DynamoDbClient> clientSetUp) {
            return withSetUp((dynamoDbClient, inspectContainerResponse) -> clientSetUp.accept(dynamoDbClient));
        }

        void run(InspectContainerResponse containerInfo) {
            DynamoDbClientBuilder builder = builderSupplier.get();

            for (Consumer<DynamoDbClientBuilder> builderFunction : builders) {
                builderFunction.accept(builder);
            }

            try (val client = builder.build()) {
                for (BiConsumer<DynamoDbClient, InspectContainerResponse> consumer : clients) {
                    consumer.accept(client, containerInfo);
                }
            }
        }
    }

    private final List<Helper> helpers = new ArrayList<>();

    void run(InspectContainerResponse containerInfo) {
        for (Helper helper : helpers) {
            helper.run(containerInfo);
        }
    }

    void addSetUp(final Supplier<DynamoDbClientBuilder> builder, final Consumer<Helper> helper) {
        val helperInstance = new Helper(builder);

        helper.accept(helperInstance);

        this.helpers.add(helperInstance);
    }
}
