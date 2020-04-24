# Mongo DB Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

# Java8 MongoDbContainer for constructing a single node MongoDB replica set. To construct a multi-node MongoDB cluster, consider the [mongodb-replica-set project](https://github.com/silaev/mongodb-replica-set/)              

## Usage example

The following example shows how to create a MongoDbContainer

<!--codeinclude-->
[Creating a MongoDB container](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDbContainerTest.java) inside_block:creatingMongoDbContainer
<!--/codeinclude-->

<!--codeinclude-->
[Starting a MongoDB container](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDbContainerTest.java) inside_block:startingMongoDbContainer
<!--/codeinclude-->

#### Motivation
Implement a reusable, cross-platform, simple to install solution that doesn't depend on 
fixed ports to test MongoDB transactions.  
  
#### General info
MongoDB starting form version 4 supports multi-document transactions only for a replica set.
For instance, to initialize a single and simple node replica set on fixed ports via Docker, one has to do the following:

* Run a MongoDB container of version 4 and up specifying --replSet command
* Initialize a single replica set via executing a proper command
* Wait for the initialization to complete
* Provide a special url for a user to employ with a MongoDB driver without specifying replicaSet

As we can see, there is a lot of operations to execute and we even haven't touched a non-fixed port approach.
That's where the MongoDbContainer might come in handy. 

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

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

!!! hint
Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency
    
#### Copyright
Copyright (c) 2019 Konstantin Silaev <silaev256@gmail.com>
