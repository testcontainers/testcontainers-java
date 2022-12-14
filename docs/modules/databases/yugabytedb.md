# YugabyteDB Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

See [Database containers](./index.md) for documentation and usage that is common to all database container types.

YugabyteDB supports two APIs. 
- Yugabyte Structured Query Language [YSQL](https://docs.yugabyte.com/latest/api/ysql/) is a fully-relational API that is built by the PostgreSQL code
- Yugabyte Cloud Query Language [YCQL](https://docs.yugabyte.com/latest/api/ycql/) is a semi-relational SQL API that has its roots in the Cassandra Query Language

## Usage example

### YSQL API 

<!--codeinclude-->
[Creating a YSQL container](../../../modules/yugabytedb/src/test/java/org/testcontainers/junit/yugabytedb/YugabyteDBYSQLTest.java) inside_block:creatingYSQLContainer
<!--/codeinclude-->


<!--codeinclude-->
[Starting a YSQL container](../../../modules/yugabytedb/src/test/java/org/testcontainers/junit/yugabytedb/YugabyteDBYSQLTest.java) inside_block:startingYSQLContainer
<!--/codeinclude-->


### YCQL API

<!--codeinclude-->
[Creating a YCQL container](../../../modules/yugabytedb/src/test/java/org/testcontainers/junit/yugabytedb/YugabyteDBYCQLTest.java) inside_block:creatingYCQLContainer
<!--/codeinclude-->


<!--codeinclude-->
[Starting a YCQL container](../../../modules/yugabytedb/src/test/java/org/testcontainers/junit/yugabytedb/YugabyteDBYCQLTest.java) inside_block:startingYCQLContainer
<!--/codeinclude-->


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:yugabytedb:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>yugabytedb</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```

!!! hint
    Adding this Testcontainers library JAR will not automatically add the Yugabytedb driver JAR to your project.
    You should ensure that your project has the Yugabytedb driver as a dependency, if you plan on using it.
    Refer to the driver page [YSQL](https://docs.yugabyte.com/latest/integrations/jdbc-driver/) and [YCQL](https://docs.yugabyte.com/latest/reference/drivers/ycql-client-drivers/) for instructions.
