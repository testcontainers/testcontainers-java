# MongoDB Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

The MongoDB module provides two Testcontainers for MongoDB unit testing:

* [MongoDBContainer](#mongodbcontainer) - the core MongoDB database
* [MongoDBAtlasLocalContainer](#mongodbatlaslocalcontainer) - the core MongoDB database combined with MongoDB Atlas Search + Atlas Vector Search

## MongoDBContainer

### Usage example

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

## MongoDBAtlasLocalContainer

### Usage example

The following example shows how to create a MongoDBAtlasLocalContainer:

<!--codeinclude-->
[Creating a MongoDB Atlas Local Container](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDBAtlasLocalContainerTest.java) inside_block:creatingAtlasLocalContainer
<!--/codeinclude-->

And how to start it:

<!--codeinclude-->
[Start the Container](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDBAtlasLocalContainerTest.java) inside_block:startingAtlasLocalContainer
<!--/codeinclude-->

It is important to use the connection string provided by the MongoDBAtlasLocalContainer's getConnectionString() method. It includes the correct host and port + the directConnection parameter:

<!--codeinclude-->
[Get the Connection String](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDBAtlasLocalContainerTest.java) inside_block:getConnectionStringAtlasLocalContainer
<!--/codeinclude-->

e.g. `mongodb://localhost:12345/?directConnection=true`

### General info
MongoDB Atlas Local combines the MongoDB database engine with MongoT, a sidecar process for advanced searching capabilities built by MongoDB and powered by [Apache Lucene](https://lucene.apache.org/). 

It allows you to use the following features:

* MongoDB Atlas Search: Atlas Search gives MongoDB queries access to the incredible search toolbox that is Lucene. The main use-case is advanced lexical text querying capabilities similar to those found in many search engines. In addition, Atlas Search supports queries with faceting and parallel index search. These can extend MongoDB's aggregation capabilities and performance for uses like statistics and complex filters.  
  [https://www.mongodb.com/docs/atlas/atlas-search/](https://www.mongodb.com/docs/atlas/atlas-search/)
* MongoDB Atlas Vector Search: Supports artificial intelligence (AI) based searches for semantically similar items in your data. Vector indexes store embeddings (high-dimensional vectors encoding semantic meaning) used in large language models (LLMs). This feature makes use of Lucene's [vector search](https://lucene.apache.org/core/9_10_0/core/org/apache/lucene/search/KnnVectorQuery.html) capabilities to find the nearness between the values of each vector. This can be a powerful alternative or compliment to lexical text search.  
  [https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/](https://www.mongodb.com/docs/atlas/atlas-vector-search/vector-search-overview/)

Pairing these Lucene backed technologies with your MongoDB database allows you to build powerful search capabilities into your applications without the need to manage a separate search engine. You can also extend your search capabilities to include AI based vector searches, which can be useful for recommendation engines, image search, and other applications that require similarity searches.

The container (mongodb/mongodb-atlas-local) documentation can be found here:
[https://www.mongodb.com/docs/atlas/cli/current/atlas-cli-deploy-docker/](https://www.mongodb.com/docs/atlas/cli/current/atlas-cli-deploy-docker/)

### Container Healthcheck
You cannot start calling Atlas Search commands, such as creating Atlas Search indexes, until the container is ready. The container takes some seconds to attain readiness, whilst:

* MongoDB database starts
* MongoDB initialises itself as a replica set
* MongoT starts and connects to the MongoDB database ready to follow Change Streams for indexing
* MongoDB connects to MongoT ready to perform $search and $vectorSearch queries

The MongoDBAtlasLocalContainer uses the container's `runner healthcheck` command to check for readiness.

### Creating an Atlas Search Index
Here's an example of how you could create an Atlas Search index during testing:

<!--codeinclude-->
[Example Atlas Search Index](../../../modules/mongodb/src/test/resources/atlas-local-index.json)
<!--/codeinclude-->

<!--codeinclude-->
[Creating an Atlas Search Index](../../../modules/mongodb/src/test/java/org/testcontainers/containers/AtlasLocalDataAccess.java) inside_block:initAtlasSearchIndex
<!--/codeinclude-->

A few things to note:

* You must create a collection before you can build an Atlas Search index for it
* It will take some time for the index to be built, and the driver's createSearchIndex will not wait for it to be ready. For this reason you need to implement your own check for the index state.

### Searching an Atlas Search Index
Here's an example of an Atlas Search query:

<!--codeinclude-->
[Searching an Atlas Search Index](../../../modules/mongodb/src/test/java/org/testcontainers/containers/AtlasLocalDataAccess.java) inside_block:queryAtlasSearch
<!--/codeinclude-->

A key thing to note during unit tests is that Atlas Search indexes are eventually consistent.

If you write data and then immediately try to read it back, you'll find there is about a [1 second delay](https://feedback.mongodb.com/forums/924868-atlas-search/suggestions/48502157-atlas-search-local-deployment-lucene-indexing-late) before the data is available for search. This is because the data is first written to the MongoDB database, then indexed by MongoT, and finally available for search after a refresh period.

You may need to use a technique like this to wait for the data to be available:

<!--codeinclude-->
[Write Data and Query Back (Eventually)](../../../modules/mongodb/src/test/java/org/testcontainers/containers/MongoDBAtlasLocalContainerTest.java) inside_block:writeAndReadBack
<!--/codeinclude-->



## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:mongodb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
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
