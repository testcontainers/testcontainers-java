# YugabyteDB Module

Testcontainers module for [YugabyteDB](https://hub.docker.com/r/yugabytedb/yugabyte)

See [Database containers](./index.md) for documentation and usage that is common to all database container types.

YugabyteDB supports two APIs.

- Yugabyte Structured Query Language [YSQL](https://docs.yugabyte.com/latest/api/ysql/) is a fully-relational API that is built by the PostgreSQL code
- Yugabyte Cloud Query Language [YCQL](https://docs.yugabyte.com/latest/api/ycql/) is a semi-relational SQL API that has its roots in the Cassandra Query Language

## Usage example

### YSQL API 

<!--codeinclude-->
[Creating a YSQL container](../../../modules/yugabytedb/src/test/java/org/testcontainers/junit/yugabytedb/YugabyteDBYSQLTest.java) inside_block:creatingYSQLContainer
<!--/codeinclude-->

### Testcontainers JDBC URL

`jdbc:tc:yugabyte:2.14.4.0-b26:///databasename`

See [JDBC](./jdbc.md) for documentation.

### YCQL API

<!--codeinclude-->
[Creating a YCQL container](../../../modules/yugabytedb/src/test/java/org/testcontainers/junit/yugabytedb/YugabyteDBYCQLTest.java) inside_block:creatingYCQLContainer
<!--/codeinclude-->


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-yugabytedb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-yugabytedb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add the Yugabytedb driver JAR to your project.
    You should ensure that your project has the Yugabytedb driver as a dependency, if you plan on using it.
    Refer to the driver page [YSQL](https://docs.yugabyte.com/latest/integrations/jdbc-driver/) and [YCQL](https://docs.yugabyte.com/latest/reference/drivers/ycql-client-drivers/) for instructions.
