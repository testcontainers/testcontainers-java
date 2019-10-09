# Mongo DB Module

#### This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future.

# Java8 MongoDbContainer for constructing a single node MongoDB replica set. To construct a multi-node MongoDB cluster, consider [mongodb-replica-set project](https://github.com/silaev/mongodb-replica-set/)   

#### Prerequisite

* Java 8+
* Docker Desktop
* Chart shows support for local and remote Docker

    local docker host | local docker host running tests from inside a container with mapping the Docker socket | remote docker daemon |
    |:---: | :---: | :---: |
    | + | + | + |    
            
#### Getting it

```groovy tab='Gradle'
testCompile "org.testcontainers:mongodb:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>mongodb</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```

To see logs, consider adding a slf4j implementation (Logback is recommended) if you don't have it in your application.
    
#### MongoDB versions that MongoDbContainer is constantly tested against:
version |
---------- |
4.0.12 |
4.2.0 |
 
#### Example usage (note that the MongoDbContainer is test framework agnostic)
The example of a JUnit5 test class:
```java
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.mongodb.MongoDbContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ITTest {
    private final MongoDbContainer mongoDbContainer = new MongoDbContainer(
        //"mongo:4.2.0"
    );

    @BeforeEach
    void setUp() {
        mongoDbContainer.start();
    }

    @AfterEach
    void tearDown() {
        mongoDbContainer.stop();
    }

    @Test
    void shouldTestReplicaSetUrl() {
        assertNotNull(mongoDbContainer.getReplicaSetUrl());
    }
}
```
The example of a JUnit5 test class in a Spring Boot + Spring Data application:
```java
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.mongodb.MongoDbContainer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@DataMongoTest
@ContextConfiguration(initializers = ITTest.Initializer.class)
class ITTest {
    private static final MongoDbContainer MONGO_DB_CONTAINER = new MongoDbContainer(
        //"mongo:4.2.0"
    );

    @BeforeAll
    static void setUp() {
        MONGO_DB_CONTAINER.start();
    }

    @AfterAll
    static void tearDown() {
        MONGO_DB_CONTAINER.stop();
    }

    @Test
    void shouldTestReplicaSetUrl() {
        assertNotNull(MONGO_DB_CONTAINER.getReplicaSetUrl());
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    String.format("spring.data.mongodb.uri: %s", MONGO_DB_CONTAINER.getReplicaSetUrl())
            ).applyTo(configurableApplicationContext);
        }
    }
}
``` 

#### Motivation
Implement a reusable, cross-platform, simple to install solution that doesn't depend on 
fixed ports to test MongoDB transactions. The solution should work with local and remote Docker daemon.  
  
#### General info
MongoDB starting form version 4 supports multi-document transactions only for a replica set.
For instance, to initialize a single and simple node replica set on fixed ports via Docker, one has to do the following:

* Run a MongoDB container of version 4 and up specifying --replSet command
* Initializing a single replica set via executing a proper command (depending on local or remote Docker daemon usage)
* Waiting for the initialization to complete
* Providing a special url (without the need to modify the OS host file) for a user to employ with a MongoDB driver without specifying replicaSet

As we can see, there is a lot of operations to execute and we even haven't touched a non-fixed port approach.
That's where the MongoDbContainer might come in handy. 

!!! hint
* Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency  
* To use remote Docker daemon check that [.testcontainers.properties](https://www.testcontainers.org/features/configuration/) file has `docker.client.strategy=org.testcontainers.dockerclient.EnvironmentAndSystemPropertyClientProviderStrategy`
and set your DOCKER_HOST environment variable   
    
#### Copyright
Copyright (c) 2019 Konstantin Silaev <silaev256@gmail.com>
