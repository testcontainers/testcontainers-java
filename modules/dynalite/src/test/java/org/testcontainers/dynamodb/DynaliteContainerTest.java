package org.testcontainers.dynamodb;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;

public class DynaliteContainerTest {

    private static final DockerImageName DYNALITE_IMAGE = DockerImageName.parse("quay.io/testcontainers/dynalite:v1.2.1-1");

    @Rule
    public DynaliteContainer dynamoDB = new DynaliteContainer(DYNALITE_IMAGE);

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
                    10L, 10L))
                .withTableName("foo");


        client.createTable(request);

        final TableDescription tableDescription = client.describeTable("foo").getTable();

        assertNotNull("the description is not null", tableDescription);
        assertEquals("the table has the right name", "foo", tableDescription.getTableName());
        assertEquals("the name has the right primary key", "Name", tableDescription.getKeySchema().get(0).getAttributeName());
    }
}
