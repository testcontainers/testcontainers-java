# Elasticsearch container

This module helps running [elasticsearch](https://www.elastic.co/products/elasticsearch) using
Testcontainers.

Note that it's based on the [official Docker image](https://www.elastic.co/guide/en/elasticsearch/reference/6.3/docker.html) provided by elastic.

## Usage example

You can start an elasticsearch container instance from any Java application by using:

```java
// Create the elasticsearch container.
ElasticsearchContainer container = new ElasticsearchContainer();

// Optional but highly recommended: Specify the version you need.
container.withVersion("6.3.2");

// Optional: you can also set what is the Docker registry you want to use with.
container.withBaseUrl("docker.elastic.co/elasticsearch/elasticsearch");

// Start the container. This step might take some time...
container.start();

// Do whatever you want here.
final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials("elastic", "changeme"));
RestClient client = RestClient.builder(container.getHost())
        .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider))
        .build();
Response response = client.performRequest("GET", "/");

// Stop the container.
container.stop();
```

Note that if you are still using the [TransportClient](https://www.elastic.co/guide/en/elasticsearch/client/java-api/6.3/transport-client.html)
(not recommended as deprecated), the default cluster name is set to `docker-cluster` so you need to change `cluster.name` setting
or set `client.transport.ignore_cluster_name` to `true`.

## Choose your Elasticsearch license

If you prefer to start a Docker image with the pure OSS version (which means with no security or
other advanced features), you can use this baseUrl instead: `docker.elastic.co/elasticsearch/elasticsearch-oss`.

