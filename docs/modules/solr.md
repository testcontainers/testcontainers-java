# Solr Container

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.


This module helps running [solr](https://lucene.apache.org/solr/) using Testcontainers.

Note that it's based on the [official Docker image](https://hub.docker.com/_/solr/).

## Usage example

You can start a solr container instance from any Java application by using:

```java
// Create the solr container.
SolrContainer container = new SolrContainer();

// Start the container. This step might take some time...
container.start();

// Do whatever you want with the client ...
SolrClient client = new Http2SolrClient.Builder("http://localhost:" + container.getSolrPort() + "/solr").build();
SolrPingResponse response = client.ping("dummy");

// Stop the container.
container.stop();
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:solr:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>solr</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
