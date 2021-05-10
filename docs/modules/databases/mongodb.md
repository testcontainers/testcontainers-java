# MongoDB Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

## Usage example

The following example shows how to create a MongoDBContainer:

<!--codeinclude-->
[Creating a MongoDB container](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDBContainerTest.java) inside_block:creatingMongoDBContainer
<!--/codeinclude-->

And how to start it:

<!--codeinclude-->
[Starting a MongoDB container](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDBContainerTest.java) inside_block:startingMongoDBContainer
<!--/codeinclude-->

!!! note
    To construct a multi-node MongoDB cluster, consider the [mongodb-replica-set project](https://github.com/silaev/mongodb-replica-set/)     

#### Motivation
Implement a reusable, cross-platform, simple to install solution that doesn't depend on 
fixed ports to test MongoDB transactions.  
  
#### General info
MongoDB starting from version 4 supports multi-document transactions only for a replica set.
For instance, to initialize a single node replica set on fixed ports via Docker, one has to do the following:

* Run a MongoDB container of version 4 and up specifying --replSet command
* Initialize a single replica set via executing a proper command
* Wait for the initialization to complete
* Provide a special url for a user to employ with a MongoDB driver without specifying replicaSet

As we can see, there is a lot of operations to execute and we even haven't touched a non-fixed port approach.
That's where the MongoDBContainer might come in handy. 

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:mongodb:{{latest_version}}"
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
