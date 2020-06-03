# Elasticsearch container

This module helps running [elasticsearch](https://www.elastic.co/products/elasticsearch) using
Testcontainers.

Note that it's based on the [official Docker image](https://www.elastic.co/guide/en/elasticsearch/reference/6.3/docker.html) provided by elastic.

## Usage example

You can start an elasticsearch container instance from any Java application by using:

<!--codeinclude-->
[HttpClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientContainer
[TransportClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:transportClientContainer
<!--/codeinclude-->


Note that if you are still using the [TransportClient](https://www.elastic.co/guide/en/elasticsearch/client/java-api/6.3/transport-client.html)
(not recommended as it is deprecated), the default cluster name is set to `docker-cluster` so you need to change `cluster.name` setting
or set `client.transport.ignore_cluster_name` to `true`.

## Choose your Elasticsearch license

If you prefer to start a Docker image with the pure OSS version (which means with no security in older versions or
other new and advanced features), you can use this instead:

<!--codeinclude-->
[Elasticsearch OSS](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:oosContainer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:elasticsearch:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
