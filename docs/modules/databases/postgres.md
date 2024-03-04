# Postgres Module

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

## Compatible images

`PostgreSQLContainer` can also be used with the following images:

* [pgvector/pgvector](https://hub.docker.com/r/pgvector/pgvector)

<!--codeinclude-->
[Using pgvector](../../../modules/postgresql/src/test/java/org/testcontainers/containers/CompatibleImageTest.java) inside_block:pgvectorContainer
<!--/codeinclude-->

* [postgis/postgis](https://registry.hub.docker.com/r/postgis/postgis)

<!--codeinclude-->
[Using PostGIS](../../../modules/postgresql/src/test/java/org/testcontainers/containers/CompatibleImageTest.java) inside_block:postgisContainer
<!--/codeinclude-->

* [timescale/timescaledb](https://hub.docker.com/r/timescale/timescaledb)

<!--codeinclude-->
[Using TimescaleDB](../../../modules/postgresql/src/test/java/org/testcontainers/containers/CompatibleImageTest.java) inside_block:timescaledbContainer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:postgresql:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.


