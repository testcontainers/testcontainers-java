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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Image is not compatible with the latest Docker version provided by GH Actions")
public class DynaliteContainerTest {

    private static final DockerImageName DYNALITE_IMAGE = DockerImageName.parse(
        "quay.io/testcontainers/dynalite:v1.2.1-1"
    );

    @Rule
    public TestcontainersRule<DynaliteContainer> dynamoDB = new TestcontainersRule<>(
        new DynaliteContainer(DYNALITE_IMAGE)
    );

    @Test
    public void simpleTestWithManualClientCreation() {
        final AmazonDynamoDB client = AmazonDynamoDBClientBuilder
            .standard()
            .withEndpointConfiguration(dynamoDB.get().getEndpointConfiguration())
            .withCredentials(dynamoDB.get().getCredentials())
            .build();

        runTest(client);
    }

    @Test
    public void simpleTestWithProvidedClient() {
        final AmazonDynamoDB client = dynamoDB.get().getClient();

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
