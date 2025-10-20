# Solr Container

This module helps running [solr](https://solr.apache.org/) using Testcontainers.

Note that it's based on the [official Docker image](https://hub.docker.com/_/solr/).

## Usage example

You can start a solr container instance from any Java application by using:

<!--codeinclude-->
[Using a Solr container](../../modules/solr/src/test/java/org/testcontainers/solr/SolrContainerTest.java) inside_block:solrContainerUsage
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-solr:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-solr</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
