package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class DynamoDBSetUpBuilder {

    @RequiredArgsConstructor(access = AccessLevel.PROTECTED)
    public static class Helper {

        @NonNull
        private DynamoDbClientBuilder builder;

        private Function<DynamoDbClientBuilder, DynamoDbClientBuilder> builderSetUp;

        private BiConsumer<DynamoDbClient, InspectContainerResponse> clientSetUp;

        public Helper withClient(final Consumer<DynamoDbClientBuilder> builderSetUp) {
            return withClient(builder -> {
                builderSetUp.accept(builder);
                return builder;
            });
        }

        public Helper withClient(final Function<DynamoDbClientBuilder, DynamoDbClientBuilder> builderSetUp) {
            this.builderSetUp = builderSetUp;
            return this;
        }

        public Helper withSetUp(final BiConsumer<DynamoDbClient, InspectContainerResponse> clientSetUp) {
            this.clientSetUp = clientSetUp;
            return this;
        }

        public Helper withSetUp(final Consumer<DynamoDbClient> clientSetUp) {
            return withSetUp((dynamoDbClient, inspectContainerResponse) -> clientSetUp.accept(dynamoDbClient));
        }

        protected void validate() {
            Preconditions.checkNotNull(builderSetUp);
            Preconditions.checkNotNull(clientSetUp);
        }

        protected void run(InspectContainerResponse containerInfo) {
            try (val client = builderSetUp.apply(builder).build()) {
                clientSetUp.accept(client, containerInfo);
            }
        }
    }

    private final List<Helper> helpers = new ArrayList<>();

    public void run(InspectContainerResponse containerInfo) {
        for (Helper helper : helpers) {
            helper.run(containerInfo);
        }
    }

    public DynamoDBSetUpBuilder withSetUp(final DynamoDbClientBuilder builder, final Consumer<Helper> helper) {
        val helperInstance = new Helper(builder);

        helper.accept(helperInstance);

        helperInstance.validate();

        this.helpers.add(helperInstance);
        return this;
    }
}
