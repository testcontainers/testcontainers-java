# Kafka Containers

Testcontainers can be used to automatically instantiate and manage [Apache Kafka](https://kafka.apache.org) containers.
More precisely Testcontainers uses the official Docker images for [Confluent OSS Platform](https://hub.docker.com/r/confluentinc/cp-kafka/)

## Benefits

* Running a single node Kafka installation with just one line of code
* No need to manage external Zookeeper installation, required by Kafka. But see [below](#zookeeper)

## Example

The following field in your JUnit test class will prepare a container running Kafka:
```java
@Rule
public KafkaContainer kafka = new KafkaContainer();
```
        
Now your tests or any other process running on your machine can get access to running Kafka broker by using the following bootstrap server location:
```java
kafka.getBootstrapServers()
```

## Options

### Selecting Kafka version

You can select a version of Confluent Platform by passing it to the container's constructor:
```java
new KafkaContainer("4.1.2")
```
The correspondence between Confluent Platform versions and Kafka versions can be seen [in Confluent documentation](https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility)
        
### <a name="zookeeper"></a> Using external Zookeeper

If for some reason you want to use an externally running Zookeeper, then just pass its location during construction:
```java
new KafkaContainer().withExternalZookeeper("localhost:2181")
```

## Multi-container usage

If your test needs to run some other Docker container which needs access to the Kafka, do the following:

* Run you other container on the same network as Kafka container. E.g. as following:
```java
new GenericContainer("myImage").withNetwork(kafka.getNetwork())
```
* Use `kafka.getNetworkAliases().get(0)+":9092"` as bootstrap server location. 
Or just give your Kafka container a network alias of your liking.

