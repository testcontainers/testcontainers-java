# Couchbase Module

<img src="https://cdn.worldvectorlogo.com/logos/couchbase.svg" width="300" />

Testcontainers module for Couchbase. [Couchbase](https://www.couchbase.com/) is a document oriented NoSQL database.

## Usage example

Running Couchbase as a stand-in in a test:

1. Define a bucket:
    <!--codeinclude-->
    [Bucket Definition](../../../modules/couchbase/src/test/java/org/testcontainers/couchbase/CouchbaseContainerTest.java) inside_block:bucket_definition
    <!--/codeinclude-->

2. define a container:
    <!--codeinclude-->
    [Container definition](../../../modules/couchbase/src/test/java/org/testcontainers/couchbase/CouchbaseContainerTest.java) inside_block:container_definition
    <!--/codeinclude-->

3. create an environment & cluster:
    <!--codeinclude-->
    [Cluster creation](../../../modules/couchbase/src/test/java/org/testcontainers/couchbase/CouchbaseContainerTest.java) inside_block:cluster_creation
    <!--/codeinclude-->

4. authenticate:
    <!--codeinclude-->
    [Authentication](../../../modules/couchbase/src/test/java/org/testcontainers/couchbase/CouchbaseContainerTest.java) inside_block:auth
    <!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:couchbase:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>couchbase</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
