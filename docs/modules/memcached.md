# Memcached Module

Memcached is an in-memory key-value cache.

## Usage example

<!--codeinclude-->
[Creating a Memcached container](../../modules/memcached/src/test/java/org/testcontainers/memcached/MemcachedContainerTest.java) inside_block:creatingContainer
<!--/codeinclude-->

<!--codeinclude-->
[Connect and operate](../../modules/memcached/src/test/java/org/testcontainers/memcached/MemcachedContainerTest.java) inside_block:connectAndOperate
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-memcached:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-memcached</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
