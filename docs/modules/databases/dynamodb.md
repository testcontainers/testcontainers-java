# DynamoDB Module

TestContainers module for [AWS DynamoDB](https://aws.amazon.com/dynamodb/).   
Amazon DynamoDB is a fully managed proprietary NoSQL database service that supports key–value and document data structures.

## Usage example

This example creates a `DynamoDbClient` which connects to the DynamoDB.

```java tab="JUnit 4 example"
public class SomeTest {

    @Rule
    public DynamoDbContainer container = new DynamoDbContainer();

    @Test
    public void test() throws URISyntaxException {
        // given
        DynamoDbClient client = DynamoDbClient.builder()
                                              .region(Region.EU_WEST_1)
                                              .endpointOverride(new URI(container.getEndpointUrl()))
                                              .build();

        // when
        ListTablesResponse tables = client.listTables();

        // then
        assertTrue(tables.tableNames().isEmpty());
    }

}
```
You can pass some configuration to `DynamoDbContainer` using the `withConfig` method.
```java tab="JUnit 4 example"
public class SomeTest {

    @Rule
    public DynamoDbContainer container = new DynamoDbContainer().withConfig(DynamoDbConfig.builder()
                                                                                          .cors("*")
                                                                                          .dbPath("/tmp/dynamodb")
                                                                                          .optimizeDbBeforeStartup(true)
                                                                                          .build());

    @Test
    public void test() throws URISyntaxException {
        // given
        DynamoDbClient client = DynamoDbClient.builder()
                                              .region(Region.EU_WEST_1)
                                              .endpointOverride(new URI(container.getEndpointUrl()))
                                              .build();

        // when
        ListTablesResponse tables = client.listTables();

        // then
        assertTrue(tables.tableNames().isEmpty());
    }

}
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:dynamodb:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>dynamodb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
