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

3. create an cluster:
    <!--codeinclude-->
    [Cluster creation](../../../modules/couchbase/src/test/java/org/testcontainers/couchbase/CouchbaseContainerTest.java) inside_block:cluster_creation
    <!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-couchbase:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-couchbase</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
