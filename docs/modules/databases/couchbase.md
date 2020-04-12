# Couchbase Module

<img src="https://cdn.worldvectorlogo.com/logos/couchbase.svg" width="300" />

Testcontainers module for Couchbase. [Couchbase](https://www.couchbase.com/) is a document oriented NoSQL database.

## Usage example

Running Couchbase as a stand-in in a test:

<!--codeinclude-->
[Basic usage](../../../modules/couchbase/src/test/java/org/testcontainers/couchbase/CouchbaseContainerTest.java) inside_block:basic_usage
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:couchbase:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>couchbase</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
