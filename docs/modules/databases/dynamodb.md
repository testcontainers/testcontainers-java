# DynamoDB Module

Testcontainers module for [AWS DynamoDB](https://aws.amazon.com/es/dynamodb/).

## Usage example

Running dynamoDbContainer as a stand-in for DynamoDB in a test:

```java
public class SomeTest {

    @Rule
    public DynamoDBContainer dynamoDbContainer = new DynamoDBContainer()
        .withInMemory()
        .withSetUpHelper(helper ->
            helper
                .withClientRegion(Region.EU_WEST_1)
                .withClientCredentials("access_key", "secret_key")
                .withSetUp(client -> createTable(client, "foo"))
        )
        .withSetUpHelper(helper ->
            helper
                .withClientRegion(Region.US_WEST_1)
                .withClientCredentials("access_key", "secret_key")
                .withSetUp(client -> createTable(client, "foo"))
                .withSetUp(client -> createTable(client, "oof"))
        );
    
    @Test
    public void someTestMethod() {
        DynamoDbClient client = dynamoDbContainer.clientBuilder()
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("access_key", "secret_key")))
            .region(Region.EU_WEST_1)
            .build();

        DescribeTableResponse response = client.describeTable(table -> table.tableName("foo"));

        Assert.assertEquals("foo", response.table().tableName());
    }
}

```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:dynamodb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>dynamodb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
