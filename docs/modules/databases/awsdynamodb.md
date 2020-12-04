# Dynalite Module

Testcontainers module for [AWS DynamoDb](https://hub.docker.com/r/amazon/dynamodb-local) official
Docker image.

## Usage example

Running the container as a stand-in for DynamoDB in a test:

```java
public class SomeTest {

    @Rule
    public DynamoDbContainer dynamoDB = new DynamoDbContainer();
    
    @Test
    public void someTestMethod() {
        // getClient() returns a preconfigured DynamoDB client that is connected to the container
        final DynamoDbClient client = dynamoDB.getClient();

        ... interact with client as if using DynamoDB normally
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

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

!!! hint
    Adding this Testcontainers library JAR will not automatically add an AWS SDK JAR to your project. You should ensure that your project also has a suitable AWS SDK JAR as a dependency.
