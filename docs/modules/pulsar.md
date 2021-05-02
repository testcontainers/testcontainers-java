# Apache Pulsar Module

Testcontainers can be used to automatically instantiate and manage [Apache Pulsar](https://pulsar.apache.org) containers.

This example connects to the Pulsar Cluster and creates a 
[Consumer](https://pulsar.apache.org/docs/en/client-libraries-java/#consumer) to read messages.

```java tab="JUnit 4 example"
public class SomeTest {

    @Rule
    public PulsarContainer cassandra = new PulsarContainer();


    @Test
    public void test(){
        try (PulsarClient pulsarClient = PulsarClient.builder().serviceUrl(pulsarContainer.getPulsarBrokerUrl()).build()) {
            try (Consumer<GenericRecord> consumer = pulsarClient.newConsumer()
                    .topic("my-topic")
                    .subscriptionName("my-subscription")
                    .subscribe()) {
                Message message;
                while ((message = consumer.receive(30, TimeUnit.SECONDS)) != null) {
                    // test the messages...
                }
            }
        }
    }

}

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:pulsar:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>pulsar</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
