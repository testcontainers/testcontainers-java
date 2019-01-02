# Testcontainers DynamoDB-Local Module

Testcontainers module for [DynamoDB Local](https://hub.docker.com/r/amazon/dynamodb-local), 
a locally runnable version of DynamoDB. More details about DynamoDB Local [here](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)

## Usage example

Running DynamoDB Local as a stand-in for DynamoDB in a test:

```java
public class SomeTest {

    @Rule
    public DynamoDbLocalContainer dynamoDB = new DynamoDbLocalContainer();

    
    @Test
    public void someTestMethod() {
        // getClient() returns a preconfigured DynamoDB client that is connected to the
        //  dynalite container
        final AmazonDynamoDB client = dynamoDB.getClient();

        ... interact with client as if using DynamoDB normally
```

### Maven

```
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>dynamodblocal</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```
compile group: 'org.testcontainers', name: 'dynamodblocal', version: 'VERSION'
```

