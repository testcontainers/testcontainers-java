package org.testcontainers.dynamodb;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.TableDescription;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;

public class DynamoDbContainerTest {

    private static final DockerImageName AWS_DYNAMODB_IMAGE = DockerImageName.parse(
        "amazon/dynamodb-local:1.13.5");

    @Rule
    public DynamoDbContainer dynamoDB = new DynamoDbContainer(AWS_DYNAMODB_IMAGE);

    @Test
    public void simpleTestWithManualClientCreation() {
        final DynamoDbClient client = DynamoDbClient.builder()
            .region(Region.AWS_GLOBAL)
            .credentialsProvider(dynamoDB.getCredentials())
            .endpointOverride(dynamoDB.getEndpointURI())
            .build();

        runTest(client);
    }

    @Test
    public void simpleTestWithProvidedClient() {
        final DynamoDbClient client = dynamoDB.getClient();

        runTest(client);
    }

    private void runTest(DynamoDbClient client) {
        CreateTableRequest request = CreateTableRequest.builder()
            .tableName("foo")
            .billingMode(BillingMode.PROVISIONED)
            .keySchema(
                KeySchemaElement.builder().keyType(KeyType.HASH).attributeName("Name").build())
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("Name")
                    .attributeType(ScalarAttributeType.S)
                    .build())
            .provisionedThroughput(
                ProvisionedThroughput.builder()
                    .readCapacityUnits(10L)
                    .writeCapacityUnits(10L)
                    .build())
            .build();

        client.createTable(request);

        final TableDescription tableDescription = client.describeTable(
            DescribeTableRequest.builder().tableName("foo").build())
            .table();

        assertNotNull("the description is not null", tableDescription);
        assertEquals("the table has the right name",
            "foo", tableDescription.tableName());
        assertEquals("the name has the right primary key",
            "Name", tableDescription.keySchema().get(0).attributeName());
    }
}
