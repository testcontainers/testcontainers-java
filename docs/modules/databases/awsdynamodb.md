# AWS DynamoDb Module

Testcontainers module for 
[AWS DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html). 
The official [amazon/dynamodb-local](https://hub.docker.com/r/amazon/dynamodb-local) Docker image 
should be used to instantiate this Testcontainer. You can check Docker Hub for the latest available 
tag [here](https://hub.docker.com/r/amazon/dynamodb-local/tags?page=1&ordering=last_updated).

## How to install

To use this 
[DynamoDB Local](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)
Testcontainer, add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:aws-dynamodb:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>aws-dynamodb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

This library does not leak any AWS dependencies. To use this Testcontainer, make sure you have the 
following dependency in your classpath 

```groovy tab='Gradle'
testCompile "software.amazon.awssdk:dynamodb"
```

```xml tab='Maven'
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>dynamodb</artifactId>
</dependency>
```

You can check the latest available version 
[here](https://mvnrepository.com/artifact/software.amazon.awssdk/dynamodb).

## How to use it

Running the container as a stand-in for DynamoDB in a test:

```java
public class SomeTest {

    @Rule
    public DynamoDbContainer dynamoDB = new DynamoDbContainer("amazon/dynamodb-local:1.13.5");
    
    @Test
    public void someTestMethod() {
        // getClient() returns a preconfigured DynamoDB client that is connected to the container
        final DynamoDbClient client = dynamoDB.getClient();

        ... interact with client as if using DynamoDB normally
```
