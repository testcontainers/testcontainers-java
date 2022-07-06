# FoundationDB Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is
    possible that it may receive breaking changes in the future. See
    [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules 
    policy.

This module helps running [FoundationDB](https://www.foundationdb.org/) using Testcontainers.

It's based on the [docker images](https://hub.docker.com/r/foundationdb/foundationdb) provided by FoundationDB 
Community.

## Usage example

You can start a FoundationDB container instance from a Java application by using:

<!--codeinclude-->
[Start FoundationDB Container](../../../modules/foundationdb/src/test/java/org/testcontainers/containers/FoundationDBContainerTest.java) inside_block:example
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:foundationdb:{{latest_version}}"
    ```

=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>foundationdb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

## Caveats

- FDB requires the native client libraries be installed separately from the java bindings. Install the libraries
  before using the java FDB client. Also, it might have issues working on newer macOS with the java bindings, try using
  java 8 and `export DYLD_LIBRARY_PATH=/usr/local/lib` in environment variables after installing FDB clients locally.
