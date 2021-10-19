# Elasticsearch container

This module helps running [elasticsearch](https://www.elastic.co/products/elasticsearch) using
Testcontainers.

Note that it's based on the [official Docker image](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html) provided by elastic.

## Usage example

You can start an elasticsearch container instance from any Java application by using:

<!--codeinclude-->
[HttpClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientContainer
[TransportClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:transportClientContainer
<!--/codeinclude-->


Note that if you are still using the [TransportClient](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/transport-client.html)
(not recommended as it is deprecated), the default cluster name is set to `docker-cluster` so you need to change `cluster.name` setting
or set `client.transport.ignore_cluster_name` to `true`.

## Secure your Elasticsearch cluster

The default distribution of Elasticsearch comes with the basic license which contains security feature.
You can turn on security by providing a password:

<!--codeinclude-->
[HttpClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientSecuredContainer
<!--/codeinclude-->

## Choose your Elasticsearch license

If you prefer to start a Docker image with the pure OSS version (which means with no security in older versions or
other new and advanced features), you can use this instead:

<!--codeinclude-->
[Elasticsearch OSS](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:ossContainer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testImplementation "org.testcontainers:elasticsearch:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
