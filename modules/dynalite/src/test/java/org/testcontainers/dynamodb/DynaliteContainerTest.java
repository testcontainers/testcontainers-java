package org.testcontainers.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import org.junit.Rule;
import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;

public class DynaliteContainerTest {

    @Rule
    public DynaliteContainer dynamoDB = new DynaliteContainer();

    @Test
    public void simpleTestWithManualClientCreation() {
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                .withEndpointConfiguration(dynamoDB.getEndpointConfiguration())
                .withCredentials(dynamoDB.getCredentials())
                .build();

        runTest(client);
    }

    @Test
    public void simpleTestWithProvidedClient() {
        final AmazonDynamoDB client = dynamoDB.getClient();

        runTest(client);
    }

    private void runTest(AmazonDynamoDB client) {
        CreateTableRequest request = new CreateTableRequest()
                .withAttributeDefinitions(new AttributeDefinition(
                        "Name", ScalarAttributeType.S))
                .withKeySchema(new KeySchemaElement("Name", KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput(
                        new Long(10), new Long(10)))
                .withTableName("foo");


        client.createTable(request);

        final TableDescription tableDescription = client.describeTable("foo").getTable();

        assertNotNull("the description is not null", tableDescription);
        assertEquals("the table has the right name", "foo", tableDescription.getTableName());
        assertEquals("the name has the right primary key", "Name", tableDescription.getKeySchema().get(0).getAttributeName());
    }
}