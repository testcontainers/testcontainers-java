package org.testcontainers.containers;

import lombok.val;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.CreateTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.io.IOException;

public class DynamoDBContainerTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void checkInMemoryParameterIsAddedAndValid() {
        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.withInMemory().start();

            Assert.assertTrue(ArrayUtils.contains(container.getCommandParts(), "-inMemory"));
            Assert.assertFalse(ArrayUtils.contains(container.getCommandParts(), "-dbPath"));
        }
    }

    @Test
    public void checkPreconditionFailsIfFieldDBPathIsNullWithFlagOptimizeDbBeforeStartup() {
        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            Assert.assertThrows(
                ContainerLaunchException.class,
                () -> container.withEnableOptimizeDbBeforeStartup().start()
            );
        }
    }

    @Test
    public void checkCorsFormatsAllowedDomainsCorrectly() {
        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.withCors("foo.com", "testcontainers.org").start();

            Assert.assertTrue(ArrayUtils.contains(container.getCommandParts(), "-cors"));
            Assert.assertTrue(ArrayUtils.contains(container.getCommandParts(), "foo.com,testcontainers.org"));
        }

        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.withCors("testcontainers.org").start();

            Assert.assertTrue(ArrayUtils.contains(container.getCommandParts(), "-cors"));
            Assert.assertTrue(ArrayUtils.contains(container.getCommandParts(), "testcontainers.org"));
        }
    }

    @Test
    public void shouldCreateSameTableInDifferentRegions() {
        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.start();

            try (val client = builderClient(container).region(Region.US_WEST_1).build()) {
                createTable(client, "foo");

                val response = client.describeTable(table -> table.tableName("foo"));

                Assert.assertEquals("foo", response.table().tableName());
            }

            try (val client = builderClient(container).region(Region.US_WEST_2).build()) {
                createTable(client, "foo");

                val response = client.describeTable(table -> table.tableName("foo"));

                Assert.assertEquals("foo", response.table().tableName());
            }
        }
    }

    @Test
    public void shouldDescribeTableInDifferentRegionsWithSharedDB() {
        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.withEnableSharedDB().start();

            try (val client = builderClient(container).region(Region.EU_WEST_1).build()) {
                createTable(client, "foo");
            }

            try (val client = builderClient(container).region(Region.US_WEST_1).build()) {
                val response = client.describeTable(table -> table.tableName("foo"));

                Assert.assertEquals("foo", response.table().tableName());
            }

            try (val client = builderClient(container).region(Region.AP_EAST_1).build()) {
                val response = client.describeTable(table -> table.tableName("foo"));

                Assert.assertEquals("foo", response.table().tableName());
            }
        }
    }

    @Test
    public void shouldShareDBBetweenExecutionsWithDBPath() throws IOException {
        val tmp = tempFolder.newFolder("dynamo_db");

        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.withFilePath(tmp, "/dynamo_db").start();

            try (val client = builderClient(container).region(Region.EU_WEST_1).build()) {
                createTable(client, "foo");

                val response = client.describeTable(table -> table.tableName("foo"));

                Assert.assertEquals("foo", response.table().tableName());
            }
        }

        try (val container = new DynamoDBContainer(DynamoDBTestImages.AWS_DYNAMODB_IMAGE)) {
            container.withFilePath(tmp, "/dynamo_db").start();

            try (val client = builderClient(container).region(Region.EU_WEST_1).build()) {
                val response = client.describeTable(table -> table.tableName("foo"));

                Assert.assertEquals("foo", response.table().tableName());
            }
        }
    }

    public static DynamoDbClientBuilder builderClient(final DynamoDBContainer container) {
        return DynamoDbClient
            .builder()
            .endpointOverride(container.getEndpointUri())
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")));
    }

    @SuppressWarnings("unchecked")
    public static CreateTableResponse createTable(DynamoDbClient client, String tableName) {
        return client.createTable(table -> {
            table
                .keySchema(sh -> sh.keyType(KeyType.HASH).attributeName("id"))
                .attributeDefinitions(df -> df.attributeName("id").attributeType(ScalarAttributeType.S))
                .tableName(tableName)
                .provisionedThroughput(builder -> builder.readCapacityUnits(1L).writeCapacityUnits(1L));
        });
    }
}
