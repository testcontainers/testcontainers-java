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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Disabled("Image is not compatible with the latest Docker version provided by GH Actions")
public class DynaliteContainerTest {

    private static final DockerImageName DYNALITE_IMAGE = DockerImageName.parse(
        "quay.io/testcontainers/dynalite:v1.2.1-1"
    );

    @Container
    public DynaliteContainer dynamoDB = new DynaliteContainer(DYNALITE_IMAGE);

    @Test
    public void simpleTestWithManualClientCreation() {
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
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
            .withAttributeDefinitions(new AttributeDefinition("Name", ScalarAttributeType.S))
            .withKeySchema(new KeySchemaElement("Name", KeyType.HASH))
            .withProvisionedThroughput(new ProvisionedThroughput(10L, 10L))
            .withTableName("foo");

        client.createTable(request);

        final TableDescription tableDescription = client.describeTable("foo").getTable();

        assertThat(tableDescription).as("the description is not null").isNotNull();
        assertThat(tableDescription.getTableName()).as("the table has the right name").isEqualTo("foo");
        assertThat(tableDescription.getKeySchema().get(0).getAttributeName())
            .as("the name has the right primary key")
            .isEqualTo("Name");
    }
}
