package org.testcontainers.scylladb;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ScyllaDBContainerTest {

    private static final DockerImageName SCYLLADB_IMAGE = DockerImageName.parse("scylladb/scylla:5.2.9");

    private static final String BASIC_QUERY = "SELECT release_version FROM system.local";

    @Test
    public void testSimple() {
        try ( // container {
            ScyllaDBContainer scylladb = new ScyllaDBContainer("scylladb/scylla:5.2.9")
            // }
        ) {
            scylladb.start();
            // session {
            CqlSession session = CqlSession
                .builder()
                .addContactPoint(scylladb.getContactPoint())
                .withLocalDatacenter("datacenter1")
                .build();
            // }
            ResultSet resultSet = session.execute(BASIC_QUERY);
            assertThat(resultSet.wasApplied()).isTrue();
            assertThat(resultSet.one().getString(0)).isNotNull();
            assertThat(session.getMetadata().getNodes().values()).hasSize(1);
        }
    }

    @Test
    public void testShardAwareness() {
        try (ScyllaDBContainer scylladb = new ScyllaDBContainer(SCYLLADB_IMAGE)) {
            scylladb.start();
            // shardAwarenessSession {
            CqlSession session = CqlSession
                .builder()
                .addContactPoint(scylladb.getShardAwareContactPoint())
                .withLocalDatacenter("datacenter1")
                .build();
            // }
            ResultSet resultSet = session.execute("SELECT driver_name FROM system.clients");
            assertThat(resultSet.one().getString(0)).isNotNull();
            assertThat(session.getMetadata().getNodes().values()).hasSize(1);
        }
    }

    @Test
    public void testAlternator() {
        try ( // alternator {
            ScyllaDBContainer scylladb = new ScyllaDBContainer(SCYLLADB_IMAGE).withAlternator()
            // }
        ) {
            scylladb.start();

            // dynamodDbClient {
            DynamoDbClient client = DynamoDbClient
                .builder()
                .endpointOverride(URI.create(scylladb.getAlternatorEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .region(Region.US_EAST_1)
                .build();
            // }
            client.createTable(
                CreateTableRequest
                    .builder()
                    .tableName("demo_table")
                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                    .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("id").attributeType(ScalarAttributeType.S).build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build()
            );
            assertThat(client.listTables().tableNames()).containsExactly(("demo_table"));
        }
    }

    @Test
    public void throwExceptionWhenAlternatorDisabled() {
        try (ScyllaDBContainer scylladb = new ScyllaDBContainer(SCYLLADB_IMAGE)) {
            scylladb.start();
            assertThatThrownBy(scylladb::getAlternatorEndpoint)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Alternator is not enabled");
        }
    }
}
