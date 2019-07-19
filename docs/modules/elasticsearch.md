# Elasticsearch container

This module helps running [elasticsearch](https://www.elastic.co/products/elasticsearch) using
Testcontainers.

Note that it's based on the [official Docker image](https://www.elastic.co/guide/en/elasticsearch/reference/6.3/docker.html) provided by elastic.

## Usage example

You can start an elasticsearch container instance from any Java application by using:

```java
// Create the elasticsearch container.
ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:6.4.1");

// Start the container. This step might take some time...
container.start();

// Do whatever you want with the rest client ...
final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress()))
        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
        .build();
Response response = restClient.performRequest(new Request("GET", "/"));

// ... or the transport client
TransportAddress transportAddress = new TransportAddress(container.getTcpHost());
Settings settings = Settings.builder().put("cluster.name", "docker-cluster").build();
TransportClient transportClient = new PreBuiltTransportClient(settings)
    .addTransportAddress(transportAddress);
ClusterHealthResponse healths = transportClient.admin().cluster().prepareHealth().get();

// Stop the container.
container.stop();
```

Note that if you are still using the [TransportClient](https://www.elastic.co/guide/en/elasticsearch/client/java-api/6.3/transport-client.html)
(not recommended as deprecated), the default cluster name is set to `docker-cluster` so you need to change `cluster.name` setting
or set `client.transport.ignore_cluster_name` to `true`.

## Choose your Elasticsearch license

If you prefer to start a Docker image with the pure OSS version (which means with no security or
other advanced features), you can use this instead:

```java
// Create the elasticsearch container.
ElasticsearchContainer container = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:6.4.1");
```

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
