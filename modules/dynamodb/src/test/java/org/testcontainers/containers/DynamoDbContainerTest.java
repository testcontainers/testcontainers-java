package org.testcontainers.containers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DescribeTableResponse;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.net.URISyntaxException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@AllArgsConstructor
@RunWith(Parameterized.class)
public class DynamoDbContainerTest {

    private final TestArgument argument;

    @Test
    public void shouldCheckDockerArguments() {
        try {
            // given
            argument.input.container.start();

            // when
            String[] commandParts = argument.input.container.getCommandParts();

            // then
            assertEquals("exposed port must be one port",
                1,
                argument.input.container.getExposedPorts().size());
            assertEquals("exposed port must be configured correctly",
                argument.expected.port,
                argument.input.container.getExposedPorts().get(0), 0);
            assertArrayEquals("docker commands must have correct arguments",
                argument.expected.dockerCommands,
                commandParts);
        } finally {
            argument.input.container.stop();
        }
    }

    @Test
    public void shouldCreateTable() {
        try {
            // given
            argument.input.container.start();
            DynamoDbClient client = buildDynamoDbClient(argument.input.container);

            // given
            client.createTable(CreateTableRequest.builder()
                .tableName("test")
                .provisionedThroughput(ProvisionedThroughput.builder()
                    .readCapacityUnits(5L)
                    .writeCapacityUnits(5L)
                    .build())
                .keySchema(KeySchemaElement.builder()
                    .attributeName("Name")
                    .keyType(KeyType.HASH)
                    .build())
                .attributeDefinitions(AttributeDefinition.builder()
                    .attributeName("Name")
                    .attributeType(ScalarAttributeType.S)
                    .build())
                .build());

            // when
            DescribeTableResponse tableResponse = client.describeTable(DescribeTableRequest
                .builder()
                .tableName("test")
                .build());

            // then
            assertNotNull("table response must not be null", tableResponse);
            assertNotNull("table must not be null", tableResponse.table());
        } finally {
            argument.input.container.stop();
        }
    }

    private DynamoDbClient buildDynamoDbClient(final DynamoDbContainer dynamoDbContainer) {
        try {
            return DynamoDbClient.builder()
                .region(Region.EU_WEST_1)
                .endpointOverride(new URI(dynamoDbContainer.getEndpointUrl()))
                .build();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Parameterized.Parameters(name = "{0}")
    public static TestArgument[] parameters() {
        return new TestArgument[] {
            TestArgument.builder()
                .testName("validating without arguments on constructor")
                .input(Input.builder()
                    .container(new DynamoDbContainer()).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'image version' argument on constructor")
                .input(Input.builder()
                    .container(new DynamoDbContainer("1.16.0")).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'dockerImageName' argument on constructor")
                .input(Input.builder()
                    .container(new DynamoDbContainer(DockerImageName.parse("amazon/dynamodb-local:1.15.0"))).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'port' config")
                .input(Input.builder()
                    .container(new DynamoDbContainer().withConfig(DynamoDbConfig.builder().port(5000).build())).build())
                .expected(Expected.builder()
                    .port(5000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "5000" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'inMemory' config")
                .input(Input.builder().container(
                    new DynamoDbContainer().withConfig(DynamoDbConfig.builder().inMemory(true).build())).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000", "-inMemory" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'delayTransientStatuses' config")
                .input(Input.builder()
                    .container(new DynamoDbContainer().withConfig(DynamoDbConfig.builder().delayTransientStatuses(true).build())).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000", "-delayTransientStatuses" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'dbPath' config")
                .input(Input.builder()
                    .container(new DynamoDbContainer().withConfig(DynamoDbConfig.builder().dbPath(".").build())).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000", "-dbPath", "." })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'sharedDb' config")
                .input(Input.builder()
                    .container(new DynamoDbContainer().withConfig(DynamoDbConfig.builder().sharedDb(true).build())).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000", "-sharedDb" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'cors' config")
                .input(Input.builder()
                    .container(new DynamoDbContainer().withConfig(DynamoDbConfig.builder().cors("*").build())).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000", "-cors", "*" })
                    .build())
                .build(),
            TestArgument.builder()
                .testName("validating 'optimizeDbBeforeStartup' config")
                .input(Input.builder()
                    .container(new DynamoDbContainer().withConfig(
                        DynamoDbConfig.builder().dbPath(".").optimizeDbBeforeStartup(true).build())).build())
                .expected(Expected.builder()
                    .port(8000)
                    .dockerCommands(new String[] { "-jar", "DynamoDBLocal.jar", "-port", "8000", "-dbPath", ".", "-optimizeDbBeforeStartup" })
                    .build())
                .build()
        };
    }

    @Builder
    @AllArgsConstructor
    private static class Input {
        private final DynamoDbContainer container;
    }

    @Builder
    @AllArgsConstructor
    private static class Expected {
        private final int port;
        private final String[] dockerCommands;
    }

    @Builder
    @AllArgsConstructor
    private static class TestArgument {
        private final String testName;
        private final Input input;
        private final Expected expected;

        @Override
        public String toString() {
            return testName;
        }
    }

}
