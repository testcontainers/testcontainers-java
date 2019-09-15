# Testcontainers

![Testcontainers logo](./logo.png)

## About

Testcontainers is a Java library that supports JUnit tests, providing lightweight, throwaway instances of common databases, Selenium web browsers, or anything else that can run in a Docker container.

Testcontainers make the following kinds of tests easier:

* **Data access layer integration tests**: use a containerized instance of a MySQL, PostgreSQL or Oracle database to test your data access layer code for complete compatibility, but without requiring complex setup on developers' machines and safe in the knowledge that your tests will always start with a known DB state. Any other database type that can be containerized can also be used.
* **Application integration tests**: for running your application in a short-lived test mode with dependencies, such as databases, message queues or web servers.
* **UI/Acceptance tests**: use containerized web browsers, compatible with Selenium, for conducting automated UI tests. Each test can get a fresh instance of the browser, with no browser state, plugin variations or automated browser upgrades to worry about. And you get a video recording of each test session, or just each session where tests failed.
* **Much more!** Check out the various contributed modules or create your own custom container classes using [`GenericContainer`](features/creating_container.md) as a base.

## Prerequisites

* Docker - please see [General Docker requirements](supported_docker_environment/index.md)
* A supported JVM testing framework:
    * [JUnit 4](test_framework_integration/junit_4.md) - See the [JUnit 4 Quickstart Guide](quickstart/junit_4_quickstart.md)
    * [Jupiter/JUnit 5](test_framework_integration/junit_5.md)
    * [Spock](test_framework_integration/spock.md)
    * *Or* manually add code to control the container/test lifecycle (See [hints for this approach](test_framework_integration/junit_4.md#manually-controlling-container-lifecycle))

## Maven dependencies

Testcontainers is distributed as separate JARs with a common version number:

* A core JAR file for core functionality, generic containers and docker-compose support
* A separate JAR file for each of the specialised modules. Each module's documentation describes the Maven/Gradle dependency to add to your project's build.

For the core library, the latest Maven/Gradle dependency is as follows: 

```groovy tab='Gradle'
testCompile "org.testcontainers:testcontainers:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

You can also [check the latest version available on Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.testcontainers%22).

[JitPack](jitpack_dependencies.md) builds are available for pre-release versions.

!!! warning "Shaded dependencies"
    Testcontainers uses the docker-java client library, which in turn depends on JAX-RS, Jersey and Jackson libraries. 
    These libraries in particular seem to be especially prone to conflicts with test code/application under test code. 
    As such, **these libraries are 'shaded' into the core testcontainers JAR** and relocated under `org.testcontainers.shaded` to prevent class conflicts.

## Who is using Testcontainers?

* [ZeroTurnaround](https://zeroturnaround.com) - Testing of the Java Agents, micro-services, Selenium browser automation
* [Zipkin](https://zipkin.io) - MySQL and Cassandra testing
* [Apache Gora](https://gora.apache.org) - CouchDB testing
* [Apache James](https://james.apache.org) - LDAP and Cassandra integration testing
* [StreamSets](https://github.com/streamsets/datacollector) - LDAP, MySQL Vault, MongoDB, Redis integration testing
* [Playtika](https://github.com/Playtika/testcontainers-spring-boot) - Kafka, Couchbase, MariaDB, Redis, Neo4j, Aerospike, MemSQL
* [JetBrains](https://www.jetbrains.com/) - Testing of the TeamCity plugin for HashiCorp Vault
* [Plumbr](https://plumbr.io) - Integration testing of data processing pipeline micro-services
* [Streamlio](https://streaml.io/) - Integration and Chaos Testing of our fast data platform based on Apache Puslar, Apache Bookeeper and Apache Heron.
* [Spring Session](https://projects.spring.io/spring-session/) - Redis, PostgreSQL, MySQL and MariaDB integration testing
* [Apache Camel](https://camel.apache.org) - Testing Camel against native services such as Consul, Etcd and so on
* [Instana](https://www.instana.com) - Testing agents and stream processing backends
* [eBay Marketing](https://www.ebay.com) - Testing for MySQL, Cassandra, Redis, Couchbase, Kafka, etc.
* [Skyscanner](https://www.skyscanner.net/) - Integration testing against HTTP service mocks and various data stores
* [Neo4j-OGM](https://neo4j.com/developer/neo4j-ogm/) - Testing new, reactive client implementations
* [Lightbend](https://www.lightbend.com/) - Testing [Alpakka Kafka](https://doc.akka.io/docs/alpakka-kafka/current/) and support in [Alpakka Kafka Testkit](https://doc.akka.io/docs/alpakka-kafka/current/testing.html#testing-with-kafka-in-docker)
* [Zalando SE](https://corporate.zalando.com/en) - Testing core business services
* [Europace AG](https://tech.europace.de/) - Integration testing for databases and micro services
* [Micronaut Data](https://github.com/micronaut-projects/micronaut-data/) - Testing of Micronaut Data JDBC, a database access toolkit
* [JHipster](https://www.jhipster.tech/) - Couchbase and Cassandra integration testing

## License

See [LICENSE](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/LICENSE).

## Attributions

This project includes a modified class (ScriptUtils) taken from the Spring JDBC project, adapted under the terms of the Apache license. Copyright for that class remains with the original authors.

This project was initially inspired by a [gist](https://gist.github.com/mosheeshel/c427b43c36b256731a0b) by [Moshe Eshel](https://github.com/mosheeshel).

## Copyright

Copyright (c) 2015-2019 Richard North and other authors.

See [AUTHORS](https://raw.githubusercontent.com/testcontainers/testcontainers-java/master/AUTHORS) for contributors.
