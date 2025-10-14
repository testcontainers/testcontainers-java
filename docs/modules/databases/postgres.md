# Postgres Module

Testcontainers module for [PostgresSQL](https://hub.docker.com/_/postgres)

## Usage example

You can start a PostgreSQL container instance from any Java application by using:

<!--codeinclude-->
[Container creation](../../../modules/postgresql/src/test/java/org/testcontainers/postgresql/PostgreSQLContainerTest.java) inside_block:container
<!--/codeinclude-->

See [Database containers](./index.md) for documentation and usage that is common to all relational database container types.

### Testcontainers JDBC URL

* PostgreSQL: `jdbc:tc:postgresql:9.6.8:///databasename`
* PostGIS: `jdbc:tc:postgis:9.6-2.5:///databasename`
* TimescaleDB: `jdbc:tc:timescaledb:2.1.0-pg13:///databasename`
* PGvector: `jdbc:tc:pgvector:pg16:///databasename`

See [JDBC](./jdbc.md) for documentation.

## Compatible images

`PostgreSQLContainer` can also be used with the following images:

* [pgvector/pgvector](https://hub.docker.com/r/pgvector/pgvector)

<!--codeinclude-->
[Using pgvector](../../../modules/postgresql/src/test/java/org/testcontainers/postgresql/CompatibleImageTest.java) inside_block:pgvectorContainer
<!--/codeinclude-->

* [postgis/postgis](https://registry.hub.docker.com/r/postgis/postgis)

<!--codeinclude-->
[Using PostGIS](../../../modules/postgresql/src/test/java/org/testcontainers/postgresql/CompatibleImageTest.java) inside_block:postgisContainer
<!--/codeinclude-->

* [timescale/timescaledb](https://hub.docker.com/r/timescale/timescaledb)

<!--codeinclude-->
[Using TimescaleDB](../../../modules/postgresql/src/test/java/org/testcontainers/postgresql/CompatibleImageTest.java) inside_block:timescaledbContainer
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-postgresql:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-postgresql</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add a database driver JAR to your project. You should ensure that your project also has a suitable database driver as a dependency.


