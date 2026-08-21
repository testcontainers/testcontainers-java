# Elasticsearch container

This module helps running [elasticsearch](https://www.elastic.co/products/elasticsearch) using
Testcontainers.

Note that it's based on the [official Docker image](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html) provided by elastic.

## Usage example

You can start an elasticsearch container instance from any Java application by using:

<!--codeinclude-->
[HttpClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientContainer7
[HttpClient with Elasticsearch 8](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientContainer8
[HttpClient with Elasticsearch 8 and SSL disabled](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientContainerNoSSL8
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

## Kibana container

This module also provides a `KibanaContainer` for testing with [Kibana](https://www.elastic.co/kibana).
Kibana requires a connection to Elasticsearch and `KibanaContainer` supports two modes: managed and external.

### Managed mode

In managed mode, `KibanaContainer` automatically connects to an `ElasticsearchContainer`:

<!--codeinclude-->
[Kibana with Elasticsearch](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/KibanaContainerTest.java) inside_block:managedModeCanStartAndReachElasticsearchInSameExplicitNetwork
<!--/codeinclude-->

When using managed mode with explicit networks, both containers must share the same `Network` instance.
Alternatively, you can omit the network configuration entirely, and `KibanaContainer` will do its best effort to create a shared, ad-hoc network automatically.

### External mode

In external mode, `KibanaContainer` connects to an external Elasticsearch instance via URL and using provided credentials:

<!--codeinclude-->
[Kibana with external Elasticsearch](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/KibanaContainerTest.java) inside_block:externalModeCanWorkWithUsernamePassword
<!--/codeinclude-->

For external mode with HTTPS, use `withElasticsearchCaCertificate()` to provide the CA certificate.
You can authenticate using either username/password (`withElasticsearchCredentials()`) or service account tokens (`withElasticsearchServiceAccountToken()`).

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-elasticsearch:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-elasticsearch</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
