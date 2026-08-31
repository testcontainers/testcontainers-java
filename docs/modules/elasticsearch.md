# Elasticsearch container

This module helps running [elasticsearch](https://www.elastic.co/products/elasticsearch) using
Testcontainers.

Note that it's based on the [official Docker image](https://www.elastic.co/guide/en/elasticsearch/reference/current/docker.html) provided by elastic.

## Usage example

In the following examples, we will be using the following versions:

<!--codeinclude-->
[Version 9 (recommended)](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:version_9
[Version 8 (maintained)](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:version_8
[Version 7 (deprecated)](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:version_7
<!--/codeinclude-->

From Elasticsearch 8 onwards, security and HTTPS are enabled by default. You can start a container and talk to it
with the REST client as follows:

<!--codeinclude-->
[HttpClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientLatest
<!--/codeinclude-->

### Disable TLS

HTTPS can be turned off if you do not need it:

<!--codeinclude-->
[HttpClient with TLS disabled](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientTlsDisabled
<!--/codeinclude-->

### Elasticsearch 7 (deprecated)

Elasticsearch 7 listens on HTTP and does not enable security unless you opt in with `withPassword()`.

<!--codeinclude-->
[HttpClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientV7
[HttpClient with security enabled](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:httpClientV7Secured
<!--/codeinclude-->

The [TransportClient](https://www.elastic.co/guide/en/elasticsearch/client/java-api/current/transport-client.html)
has been removed in Elasticsearch 8. It can still be used against a 7.x container. The default cluster name is
`docker-cluster`, so you need to change the `cluster.name` setting or set `client.transport.ignore_cluster_name` to `true`.

<!--codeinclude-->
[TransportClient](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:transportClientV7
<!--/codeinclude-->

### OSS distribution

The last OSS image is `elasticsearch-oss:7.10.2`. It does not include features under the Elastic License, and
`withPassword()` is rejected:

<!--codeinclude-->
[OSS image](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/ElasticsearchContainerTest.java) inside_block:ossContainer
<!--/codeinclude-->

## Kibana container

This module also provides a `KibanaContainer` for testing with [Kibana](https://www.elastic.co/kibana).
Kibana requires a connection to Elasticsearch and `KibanaContainer` supports two modes: managed and external.

### Managed mode

In managed mode, `KibanaContainer` automatically connects to an `ElasticsearchContainer`:

<!--codeinclude-->
[Kibana with Elasticsearch](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/KibanaContainerTest.java) inside_block:managedModeReachesElasticsearchOnSharedNetwork
<!--/codeinclude-->

When using managed mode with explicit networks, both containers must share the same `Network` instance.
Alternatively, you can omit the network configuration entirely, and `KibanaContainer` will do its best effort to create a shared, ad-hoc network automatically.

### External mode

In external mode, `KibanaContainer` connects to an external Elasticsearch instance via URL and using provided credentials:

<!--codeinclude-->
[Kibana with external Elasticsearch](../../modules/elasticsearch/src/test/java/org/testcontainers/elasticsearch/KibanaContainerTest.java) inside_block:externalModeReachesElasticsearchWithUsernamePassword
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
