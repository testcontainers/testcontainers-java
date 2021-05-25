# Kafka Containers

Testcontainers can be used to automatically instantiate and manage [Apache Kafka](https://kafka.apache.org) containers.
More precisely Testcontainers uses the official Docker images for [Confluent OSS Platform](https://hub.docker.com/r/confluentinc/cp-kafka/)

## Benefits

* Running a single node Kafka installation with just one line of code
* No need to manage external Zookeeper installation, required by Kafka. But see [below](#zookeeper)

## Example

The following field in your JUnit test class will prepare a container running Kafka:
<!--codeinclude-->
[JUnit Rule](../../modules/kafka/src/test/java/org/testcontainers/containers/KafkaContainerTest.java) inside_block:junitRule
<!--/codeinclude-->

The correspondence between Confluent Platform versions and Kafka versions can be seen [in Confluent documentation](https://docs.confluent.io/current/installation/versions-interoperability.html#cp-and-apache-kafka-compatibility)

Now your tests or any other process running on your machine can get access to running Kafka broker by using the following bootstrap server location:

<!--codeinclude-->
[Bootstrap Servers](../../modules/kafka/src/test/java/org/testcontainers/containers/KafkaContainerTest.java) inside_block:getBootstrapServers
<!--/codeinclude-->

## Options
        
### <a name="zookeeper"></a> Using external Zookeeper

If for some reason you want to use an externally running Zookeeper, then just pass its location during construction:
<!--codeinclude-->
[External Zookeeper](../../modules/kafka/src/test/java/org/testcontainers/containers/KafkaContainerTest.java) inside_block:withExternalZookeeper
<!--/codeinclude-->


## Multi-container usage

If your test needs to run some other Docker container which needs access to Kafka, do the following:

* Run your other container on the same network as Kafka container, e.g.:
<!--codeinclude-->
[Network](../../modules/kafka/src/test/java/org/testcontainers/containers/KafkaContainerTest.java) inside_block:withKafkaNetwork
<!--/codeinclude-->
* Use `kafka.getNetworkAliases().get(0)+":9092"` as bootstrap server location. 
Or just give your Kafka container a network alias of your liking.

You will need to explicitly create a network and set it on the Kafka container as well as on your other containers that need to communicate with Kafka.

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:kafka:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
